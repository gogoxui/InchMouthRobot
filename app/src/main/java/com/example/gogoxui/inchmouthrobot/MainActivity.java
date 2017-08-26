package com.example.gogoxui.inchmouthrobot;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.litepal.LitePal;
import org.litepal.crud.DataSupport;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private Button bt_update;
    private Button bt_listen;
    private Button bt_feedback;
    private ImageView img_show;
    private TextView tv_version;
    private TextView tv_tmp;
    private String web_resource = "";
    private float local_version;
    private float web_version;
    private String db_link;
    private TextToSpeech tts;
    private int listen_counter = 0;
    private String whatIsaid= "";
    private String saywhat= "";
    private String feedbackMail;



    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LitePal.getDatabase();
        //////////////////
        bt_update = (Button) findViewById(R.id.updata_button);
        bt_listen = (Button) findViewById(R.id.listen_button);
        bt_feedback = (Button) findViewById(R.id.feedback);
        tv_version = (TextView) findViewById(R.id.version_tv);
        img_show = (ImageView) findViewById(R.id.img_tv);
        tv_tmp = (TextView) findViewById(R.id.tmp_tv);
        //////////////////

        checkLocalDB();

        //////////////////


/////////載入TTS設定////////
        try{

            showWarnning();

            String st_language = DataSupport.find(Wordlist.class,4).getReactionWord();
            tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int i) {
                    final Locale tts_language = localeCallBack();
                    String st_speedrate = DataSupport.find(Wordlist.class,2).getReactionWord();
                    String st_pitch = DataSupport.find(Wordlist.class,3).getReactionWord();
                    float speedrate = Float.parseFloat(st_speedrate);
                    float pitch = Float.parseFloat(st_pitch);
                    int result = tts.setLanguage(tts_language);
                    if (i == TextToSpeech.SUCCESS) {
                        if (result == TextToSpeech.LANG_MISSING_DATA||result == TextToSpeech.LANG_NOT_SUPPORTED){
                            Log.e("error", "This Language is not supported");
                            Toast.makeText(MainActivity.this, "我唔識講粵語啵", Toast.LENGTH_LONG).show();
                        } else {
                            tts.setLanguage(tts_language);
                            tts.setSpeechRate((float)speedrate);
                            tts.setPitch((float)pitch);
                        }
                    }else {
                        Log.e("error", "Initilization Failed!");
                        Toast.makeText(MainActivity.this, "我識講粵語，但開不了口", Toast.LENGTH_LONG).show();
                    }
                }});

            /////Show Version/////
            String st_nowVersion = DataSupport.find(Wordlist.class,1).getReactionWord();
            local_version = Float.parseFloat(st_nowVersion);
            tv_version.setText("現在版本: "+local_version);
        }catch (Exception e){
            e.printStackTrace();
        }
//////


        bt_listen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isNetworkConnected(MainActivity.this)){
                    sayNow(new checkRobot().noNetwork());
                }else {
                    listenNow(1);
                }
            }
        });

        bt_update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DownloadUpdate(MainActivity.this,bt_update)
                        .execute("http://139.162.97.174/robot/databases/Wordlist.db");
            }
        });

        bt_feedback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                feedbackMail = "是咁的..."
                        + "\n---------------"
                        + "\n機器人聽到："+whatIsaid
                        +",\n機器人回覆："+saywhat
                        + "\n---------------"
                        + "\n你應該：\n ";

                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:you.archi.2017@gmail.com"));
                intent.putExtra(Intent.EXTRA_SUBJECT,"寸嘴機器人投訴狀");
                intent.putExtra(Intent.EXTRA_TEXT,feedbackMail);
                startActivity(intent);
            }
        });

    }

    private void checkLocalDB(){
        try{
            String st_nowVersion = DataSupport.find(Wordlist.class,1).getReactionWord();
            local_version = Float.parseFloat(st_nowVersion);
            Toast.makeText(MainActivity.this,"已載入大腦了",Toast.LENGTH_SHORT).show();
        }catch (Exception e){
            e.printStackTrace();
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("無法載入大腦")
                    .setMessage("是否要下載新大腦？")
                    .setNegativeButton("不了", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            onDestroy();
                        } })
                    .setPositiveButton("好啊", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new DownloadUpdate(MainActivity.this,bt_update)
                                    .execute("http://139.162.97.174/robot/databases/Wordlist.db");
                        } })
                    .create().show();
        }finally {
            new CheckUpdate(tv_tmp).execute();
        }

    }

    private void listenNow(int request_num){
        //透過 Intent的方式開啟內建的語音辨識 Activity...

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh_HK");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "讲野啦..."); //語音辨識 Dialog 上要顯示的提示文字


        startActivityForResult(intent,request_num);
        listenCounter();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        switch(requestCode){
            case 1:
                //////1:listen and react
                if (resultCode == RESULT_OK) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    checkRobot cr = new checkRobot();
                    whatIsaid = result.get(0);
                    saywhat =  cr.getReturnword(this,whatIsaid);
                    sayNow(saywhat);
                    changeImg();
                    break;
                }
            /*
                case 2:
                //////2:Check TTS OR Install
                if(resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS){
                Toast.makeText(getApplicationContext(),"我識講嘢", Toast.LENGTH_LONG).show();
            } else {
                Intent installIntent = new Intent();
                installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
                Toast.makeText(getApplicationContext(),"安裝Google語音引擎", Toast.LENGTH_LONG).show();
            }*/

        }


    }

    private void listenCounter(){
        int num = listen_counter;
        switch (num){
            case 0:;
                break;
        }
        listen_counter = listen_counter + 1;
    }

    private void checkWebDB(){
        web_resource = tv_tmp.getText().toString();
        //Toast.makeText(MainActivity.this,web_resource,Toast.LENGTH_SHORT).show();
        web_version = getVersion(web_resource);
        db_link = getDBLink(web_resource);
        if (web_version > local_version){
            tv_version.setText("現在版本: "+local_version+"-->有更新版本："+web_version);
            bt_update.setVisibility(View.VISIBLE);
        }

    }

    public void sayNow(String w){
        tts.speak(w, TextToSpeech.QUEUE_FLUSH,null);
    }

    public Locale localeCallBack(){
        String st_language = DataSupport.find(Wordlist.class,4).getReactionWord();
        int key = Integer.parseInt(st_language);

        Locale locale = new Locale("yue","HK");
        switch (key){
            case 1:
                locale = new Locale("yue","HK");
                break;
            case 2:
                locale = new Locale("ja","JP");
                break;
            case 3:
                locale = Locale.US;
                break;
        }
        return locale;
    }

    public float getVersion(String webData){
        float version =(float)1.0;
        String mData = webData;
        String st_version = "";
        Pattern pattern = Pattern.compile("<version>(.+?)</version>");
        Matcher matcher = pattern.matcher(mData);
        while (matcher.find()) {
            st_version =  matcher.group(1);
            version = Float.parseFloat(st_version);
        }
        return version;
    }

    public String getDBLink(String webData){
        String link ="";
        String mData = webData;
        Pattern pattern = Pattern.compile("<db_link>(.+?)</db_link>");
        Matcher matcher = pattern.matcher(mData);
        while (matcher.find()) {
            link =  matcher.group(1);
        }
        return link;
    }


    @Override
    public void onDestroy(){
        if (tts != null){
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    public void recreate(){
        this.onDestroy();
    }

    public void showWarnning(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("簽張生死狀！")
                .setMessage("本程序含有令人不安的內容！\n你確定你已年満18歲,\n且心理承受能力正常？")
                .setNegativeButton("不了!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onDestroy();
                    } })
                .setPositiveButton("當然！", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        checkWebDB();
                    } })
                .create().show();
    }

    public boolean isNetworkConnected(Context context) {
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable();
            }
        }
        return false;


    }

    public void changeImg(){
        //int numberOfFiles = new File().listFiles().length;
        //String path = "R.drawable." +num;
        Random random = new Random();
        int[] pictures = new int[]{
                R.drawable.img1,
                R.drawable.img2
        };
        img_show.setImageResource(pictures[random.nextInt(pictures.length)]);
    }

class DownloadUpdate extends AsyncTask<String, Integer, String> {
    int contentLen;//声明要下载的文件总长
    private Context mContext;
    private Button mBt;

    public DownloadUpdate(Context context, Button bt) {
        mContext = context;
        mBt = bt;
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            URL url = new URL(params[0]);
            //获取连接
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            //获取下载文件的大小
            contentLen = connection.getContentLength();
            //根据下载文件大小设置进度条最大值（使用标记区别实时进度更新）
            //循环下载（边读取边存入）
            BufferedInputStream bis = new BufferedInputStream(connection.getInputStream());
            BufferedOutputStream bos = new BufferedOutputStream(
                    new FileOutputStream(
                            Environment.getDataDirectory() + "/data/com.example.gogoxui.inchmouthrobot/databases/Wordlist.db"));
            int len = -1;
            byte[] bytes = new byte[1024];
            while ((len = bis.read(bytes)) != -1) {
                bos.write(bytes, 0, len);
                bos.flush();
            }
            bos.close();
            bis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "下载完成";
    }
    //doInBackground方法执行完后被UI thread执行
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        mBt.setVisibility(View.GONE);
        Toast.makeText(mContext, "我有腦了！Yeah~ ", Toast.LENGTH_SHORT).show();
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("更新成功")
                .setMessage("重新打開機器人")
                .setPositiveButton("好啊", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new MainActivity().recreate();
                    } })
                .create().show();
    }
}


class CheckUpdate extends AsyncTask<Void,String,Void>{
    private String web_data;
    private TextView tv;

    public CheckUpdate(TextView textView) {
        tv = textView;
    }

   @Override
   protected Void doInBackground(Void... voids) {
       try {
           web_data = downloadString("http://139.162.97.174/robot/get_data.xml");
           return null;
       }catch (Exception e){
           e.printStackTrace();
       }
       return null;
   }
    @Override
    protected void onPostExecute(Void result){
        super.onPostExecute(result);
        tv.setText(web_data);

    }

   private String downloadString(String address){
       String webData = "";
       try{

           URL url = new URL(address);
           HttpURLConnection connection = (HttpURLConnection) url.openConnection();
           connection.setReadTimeout(10000);
           connection.setConnectTimeout(20000);
           connection.setRequestMethod("GET");
           connection.setInstanceFollowRedirects(false);
           connection.setRequestProperty("Accept-Language","en");
           connection.connect();
           int resq = connection.getResponseCode();

           switch (resq){
               case HttpURLConnection.HTTP_OK:
                   InputStream is = connection.getInputStream();
                   webData = convertStreamToString(is);
                   is.close();
                   break;
           }

       }catch (Exception e){
           e.printStackTrace();
       }

       return webData;
   }

   private String convertStreamToString(InputStream is) {
       BufferedReader reader = new BufferedReader(new InputStreamReader(is));
       StringBuilder sb = new StringBuilder();

       String line = null;
       try {
           while ((line = reader.readLine()) != null) {
               sb.append(line + "/n");
           }
       } catch (IOException e) {
           e.printStackTrace();
       } finally {
           try {
               is.close();
           } catch (IOException e) {
               e.printStackTrace();
           }
       }

       return sb.toString();
   }

}

}


