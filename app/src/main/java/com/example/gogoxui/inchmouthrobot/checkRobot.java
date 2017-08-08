package com.example.gogoxui.inchmouthrobot;

import android.content.Context;
import android.widget.Toast;

import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by gogoxui on 17-8-6.
 */

public class checkRobot {
    private String inputword;
    //private String trigger;
    private String returnword ;

    private List<Wordlist> wls = DataSupport.findAll(Wordlist.class);

    public String getReturnword(Context context, String iw){
        returnword = "乜料呀";
        inputword = iw;
        ArrayList<String> resultList = new ArrayList<String>();
        Toast.makeText(context,"Input is:"+iw,Toast.LENGTH_SHORT).show();
        for (Wordlist wl:wls){
            Pattern pattern = Pattern.compile(wl.getTriggerWord());
            Matcher matcher = pattern.matcher(iw);
            if (matcher.matches()){
                String newword = matcher.replaceAll(wl.getReactionWord());
                //Toast.makeText(context,"Trigger is:"+wl.getTriggerWord()+"\nReactioni is:"+newword,Toast.LENGTH_LONG).show();
                resultList.add(newword);
            }}


        int listSize = resultList.size();
        int wordOfList = (int) (Math.random()*listSize);

        if (listSize>=1){
            returnword = resultList.get(wordOfList);
            //Toast.makeText(context,"Say:"+returnword,Toast.LENGTH_LONG).show();
        }else{
            //Toast.makeText(context,"無找到結果",Toast.LENGTH_LONG).show();
            returnword = dontKnowWhat();
        }

        return returnword;
    }

    private String dontKnowWhat(){
        String unknowWord;
        List<Wordlist> wl_unknowWord = DataSupport.where("name = ?","noResult").find(Wordlist.class);
        int nwListSize = wl_unknowWord.size();
        int numOfList = (int) (Math.random()*nwListSize);
        unknowWord = wl_unknowWord.get(numOfList).getReactionWord();
        return unknowWord;
    }

    public String noNetwork(){
        String noNetworkWord;
        List<Wordlist> wl_noNW = DataSupport.where("name = ?","noNetwork").find(Wordlist.class);
        int nwListSize = wl_noNW.size();
        int numOfList = (int) (Math.random()*nwListSize);
        noNetworkWord = wl_noNW.get(numOfList).getReactionWord();
        return noNetworkWord;
    }
}
