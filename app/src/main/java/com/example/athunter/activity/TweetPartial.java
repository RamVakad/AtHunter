package com.example.athunter.activity;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.athunter.R;

public class TweetPartial extends RecyclerView.ViewHolder {
    TextView tweetTextView;
    ImageView tweetImage;
    TextView tweetTime;
    private boolean isImageFitToScreen = false;

    TweetPartial(View v) {
        super(v);
        tweetTextView = itemView.findViewById(R.id.tweetText);
        tweetImage = itemView.findViewById(R.id.tweetImage);
        tweetTime = itemView.findViewById(R.id.tweetTime);

        tweetImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isImageFitToScreen) {
                    isImageFitToScreen=false;
                    tweetImage.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    tweetImage.setAdjustViewBounds(true);
                }else{
                    isImageFitToScreen=true;
                    tweetImage.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                    tweetImage.setScaleType(ImageView.ScaleType.FIT_XY);
                }
            }
        });
    }
}