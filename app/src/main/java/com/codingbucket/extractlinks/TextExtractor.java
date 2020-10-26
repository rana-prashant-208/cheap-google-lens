package com.codingbucket.extractlinks;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.util.SparseArray;

import androidx.annotation.RequiresApi;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TextExtractor {
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static List getTexts(Bitmap bitmap, TextRecognizer textRecognizer){
        System.out.println("handleBitMap");
        List<String> textList=new ArrayList<>();
        Frame imageFrame = new Frame.Builder()
                .setBitmap(bitmap)
                .build();

        String imageText = "";
        SparseArray<TextBlock> textBlocks = textRecognizer.detect(imageFrame);
        for (int i = 0; i < textBlocks.size(); i++) {
            TextBlock textBlock = textBlocks.valueAt(i);
            imageText = textBlock.getValue();                   // return string
            if(imageText.contains("\n")){
                String[] splits = imageText.split("\n");
                Arrays.stream(splits).forEach(s -> {
                    String[] splits2 = s.split(" ");
                    Arrays.stream(splits2).forEach(s2 -> textList.add(s2));
                });
            }else if(imageText.contains(" ")){
                String[] splits = imageText.split(" ");
                Arrays.stream(splits).forEach(s -> textList.add(s));
            }else
                {
                System.out.println("Text : "+imageText);
                textList.add(imageText);
            }
        }
        return textList;
    }
}
