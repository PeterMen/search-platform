package com.peter.search.pojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Data
public class SuggesterGeneralRecord {

    private List<String> messageList = new ArrayList<>();
    private AtomicBoolean suggestGeneralFinished = new AtomicBoolean(false);

    public void addMessage(String msg){
        messageList.add(msg);
    }

    public void setFinished(){
        suggestGeneralFinished.set(true);
    }
    public void reset(){
        messageList.clear();
        suggestGeneralFinished.set(false);
    }
}
