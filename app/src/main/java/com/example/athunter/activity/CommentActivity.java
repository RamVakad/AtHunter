package com.example.athunter.activity;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.example.athunter.R;
import com.example.athunter.global.Statics;
import com.example.athunter.global.config.AppConfig;
import com.example.athunter.model.Comment;
import com.example.athunter.model.Tweet;
import com.example.athunter.service.PermissionService;
import com.example.athunter.util.GeneralTools;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.appindexing.FirebaseAppIndex;
import com.google.firebase.appindexing.FirebaseUserActions;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class CommentActivity extends AppCompatActivity {

    private static final String TAG = "CommentActivity";

    private ProgressBar progressBar;
    private ImageView fullScreenImgView;
    private RecyclerView commentRecyclerView;
    private LinearLayoutManager linearLayoutManager;
    private DatabaseReference commentsRealtimeRef;
    private DatabaseReference databaseReference;
    private EditText commentBox;
    private Button commentButton;
    private TextView tweetText;
    private ImageView tweetImage;

    private FirebaseRecyclerAdapter<Comment, CommentPartial> recyclerAdapter;

    private FirebaseUser firebaseUser;
    private FirebaseAuth firebaseAuth;

    LocationManager locationManager;
    private boolean noLocation = false;
    private LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            // Called when a new location is found by the network location provider.
            //System.out.println(location.toString());
            Statics.CURR_LOCATION = location;
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {}

        public void onProviderEnabled(String provider) {}

        public void onProviderDisabled(String provider) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);
        progressBar = findViewById(R.id.progressBar);
        fullScreenImgView = findViewById(R.id.FullScreenImg);
        commentBox = findViewById(R.id.commentTextBox);
        commentButton = findViewById(R.id.commentButton);
        commentRecyclerView = findViewById(R.id.commentsRecyclerView);
        linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        linearLayoutManager.setReverseLayout(true);
        commentRecyclerView.setLayoutManager(linearLayoutManager);
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        if (firebaseUser == null) {
            firebaseAuth.signInAnonymously().addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (!task.isSuccessful()) {
                        Toast.makeText(CommentActivity.this, "Authentication failed.", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }




        databaseReference = FirebaseDatabase.getInstance().getReference();
        DatabaseReference tweetRef = databaseReference.child(AppConfig.REALTIME_DB_COLLECTION_NAME).child(getIntent().getStringExtra("KEY"));

        commentsRealtimeRef = tweetRef.child("comments");


        FirebaseRecyclerOptions<Comment> options = new FirebaseRecyclerOptions.Builder<Comment>().setQuery(commentsRealtimeRef, GeneralTools.getCommentParser()).build();
        recyclerAdapter = new FirebaseRecyclerAdapter<Comment, CommentPartial>(options) {
            @NonNull
            @Override
            public CommentPartial onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                progressBar.setVisibility(ProgressBar.INVISIBLE);
                LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
                return new CommentPartial(inflater.inflate(R.layout.partial_comment, viewGroup, false));
            }

            @Override
            protected void onBindViewHolder(@NonNull final CommentPartial viewHolder, int position, @NonNull Comment comment) {
                viewHolder.fullScreenImgView = fullScreenImgView;
                if (comment.getText() != null) {
                    viewHolder.commentTextView.setText(comment.getText());
                    viewHolder.commentTextView.setVisibility(TextView.VISIBLE);
                } else {
                    viewHolder.commentTextView.setVisibility(TextView.GONE);
                }



                Long millis = comment.getTime();
                Long diff = System.currentTimeMillis() - millis;
                if (diff > TimeUnit.DAYS.toMillis(1)) {
                    long days = diff / TimeUnit.DAYS.toMillis(1);
                    viewHolder.commentTime.setText(days + "d");
                } else {
                    Date date = new Date(millis);
                    SimpleDateFormat formatter = new SimpleDateFormat("h:mm a");
                    String formatted = formatter.format(date);
                    viewHolder.commentTime.setText(formatted);
                }
            }
        };

        recyclerAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int commentCount = recyclerAdapter.getItemCount();
                int lastVisiblePosition = linearLayoutManager.findLastCompletelyVisibleItemPosition();
                if (lastVisiblePosition == -1 || (positionStart >= (commentCount - 1) && lastVisiblePosition == (positionStart - 1))) {
                    commentRecyclerView.scrollToPosition(positionStart);
                }
                if (lastVisiblePosition == -1 || (positionStart >= (commentCount - 1))) {
                    //tweetRecyclerView.scrollToPosition(positionStart);
                    //Toast.makeText(CommentActivity.this, "New Comment --> Scroll Up", Toast.LENGTH_SHORT).show();
                }
            }
        });

        commentRecyclerView.setAdapter(recyclerAdapter);


        commentBox.setFilters(new InputFilter[] { new InputFilter.LengthFilter(AppConfig.MAX_TWEET_LENGTH) });
        commentBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void afterTextChanged(Editable editable) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    commentButton.setEnabled(true);
                } else {
                    commentButton.setEnabled(false);
                }
            }
        });

        commentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (noLocation) {
                    Toast.makeText(CommentActivity.this, "Location Permissions Required.", Toast.LENGTH_LONG).show();
                } else if (!(GeneralTools.isUserInRange(Statics.CURR_LOCATION.getLatitude(), Statics.CURR_LOCATION.getLongitude()))) {
                    Toast.makeText(CommentActivity.this, "You must be around Hunter College to tweet.", Toast.LENGTH_LONG).show();
                } else {
                    Comment comment = new Comment(commentBox.getText().toString(), System.currentTimeMillis());
                    commentsRealtimeRef.push().setValue(comment);
                    commentBox.setText("");
                }
            }
        });



        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } catch (SecurityException e) {
            System.out.println("No location permissions.");
            noLocation = true;
        }


        if(!PermissionService.Check_FINE_LOCATION(CommentActivity.this)) {
            PermissionService.Request_FINE_LOCATION(CommentActivity.this,22);
        }

        fullScreenImgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fullScreenImgView.setVisibility(ImageView.GONE);
            }
        });

        tweetText = findViewById(R.id.tweetText);
        tweetText.setText(getIntent().getStringExtra("tweetText"));

        tweetImage = findViewById(R.id.tweetImage);
        String imageUrl = getIntent().getStringExtra("tweetImg");
        if (imageUrl != null) {
            tweetImage.setVisibility(ImageView.VISIBLE);
            try {
                if (imageUrl.startsWith("gs://")) {
                    StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
                    storageReference.getDownloadUrl().addOnCompleteListener(
                            new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {
                                    if (task.isSuccessful()) {
                                        String downloadUrl = task.getResult().toString();
                                        Glide.with(tweetImage.getContext())
                                                .load(downloadUrl)
                                                .apply(new RequestOptions()
                                                        .fitCenter()
                                                        .format(DecodeFormat.PREFER_ARGB_8888)
                                                        .override(Target.SIZE_ORIGINAL)).into(new SimpleTarget<Drawable>(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL) {
                                            @Override
                                            public void onResourceReady(@NonNull final Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                                tweetImage.setImageDrawable(resource);
                                            }
                                        });
                                    } else {
                                        Log.w(TAG, "Getting download url was not successful.", task.getException());
                                    }
                                }
                            });
                } else {
                    Glide.with(tweetImage.getContext()).load(imageUrl).into(tweetImage);
                }
            } catch (Exception e) { }
        } else {
            tweetImage.setVisibility(ImageView.GONE);
        }

        tweetImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("Clicked.");
                fullScreenImgView.setImageDrawable(tweetImage.getDrawable());
                fullScreenImgView.setVisibility(ImageView.VISIBLE);
            }
        });

        progressBar.setVisibility(ProgressBar.INVISIBLE);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onPause() {
        recyclerAdapter.stopListening();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        recyclerAdapter.startListening();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
