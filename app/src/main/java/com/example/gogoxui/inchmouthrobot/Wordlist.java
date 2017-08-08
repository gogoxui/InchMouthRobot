package com.example.gogoxui.inchmouthrobot;

import org.litepal.crud.DataSupport;

/**
 * Created by gogoxui on 17-8-6.
 */

public class Wordlist extends DataSupport {
    private int id;
    private String name;
    private String triggerWord;
    private String reactionWord;
    //private String languageID;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTriggerWord() {
        return triggerWord;
    }

    public void setTriggerWord(String triggerWord) {
        this.triggerWord = triggerWord;
    }

    public String getReactionWord() {
        return reactionWord;
    }

    public void setReactionWord(String reactionWord) {
        this.reactionWord = reactionWord;
    }

}
