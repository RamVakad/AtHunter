package com.example.athunter.activity;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.athunter.R;

public class CommentPartial extends RecyclerView.ViewHolder  {

    TextView commentTextView;
    TextView commentTime;
    ImageView fullScreenImgView;

    CommentPartial(View v) {
        super(v);
        commentTextView = itemView.findViewById(R.id.commentText);
        commentTime = itemView.findViewById(R.id.commentTime);
    }
}
