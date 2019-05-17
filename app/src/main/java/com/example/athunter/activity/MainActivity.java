package com.example.athunter.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
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
import android.widget.LinearLayout;
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
import com.example.athunter.model.Tweet;
import com.example.athunter.global.config.AppConfig;
import com.example.athunter.service.PermissionService;
import com.example.athunter.util.GeneralTools;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.appindexing.FirebaseAppIndex;
import com.google.firebase.appindexing.FirebaseUserActions;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.vansuita.pickimage.bean.PickResult;
import com.vansuita.pickimage.bundle.PickSetup;
import com.vansuita.pickimage.dialog.PickImageDialog;
import com.vansuita.pickimage.listeners.IPickResult;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements IPickResult {

    private static final String TAG = "MainActivity";

    private ProgressBar progressBar;
    private RecyclerView tweetRecyclerView;
    private LinearLayoutManager linearLayoutManager;
    private ImageView addMediaButton;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private EditText tweetBox;
    private Button tweetButton;
    private ImageView txtBoxImgView;
    private boolean noLocation = false;

    private FirebaseUser firebaseUser;
    private DatabaseReference tweetsRealtimeRef;
    private FirebaseRecyclerAdapter<Tweet, TweetPartial> recyclerAdapter;
    private boolean imageSelected = false;
    private PickResult imagePicked;
    private ImageView fullScreenImgView;

    LocationManager locationManager;

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
        setContentView(R.layout.activity_main);
        progressBar = findViewById(R.id.progressBar);
        fullScreenImgView = findViewById(R.id.FullScreenImg);
        tweetRecyclerView = findViewById(R.id.messageRecyclerView);
        linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        linearLayoutManager.setReverseLayout(true);
        tweetRecyclerView.setLayoutManager(linearLayoutManager);
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        if (firebaseUser == null) {
            firebaseAuth.signInAnonymously().addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (!task.isSuccessful()) {
                        Toast.makeText(MainActivity.this, "Authentication failed.", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

        databaseReference = FirebaseDatabase.getInstance().getReference();
        tweetsRealtimeRef = databaseReference.child(AppConfig.REALTIME_DB_COLLECTION_NAME);

        FirebaseRecyclerOptions<Tweet> options = new FirebaseRecyclerOptions.Builder<Tweet>().setQuery(tweetsRealtimeRef, GeneralTools.getTweetParser()).build();

        recyclerAdapter = new FirebaseRecyclerAdapter<Tweet, TweetPartial>(options) {
            @NonNull
            @Override
            public TweetPartial onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
                return new TweetPartial(inflater.inflate(R.layout.partial_tweet, viewGroup, false));
            }

            @Override
            protected void onBindViewHolder(@NonNull final TweetPartial viewHolder, int position, @NonNull final Tweet tweet) {
                progressBar.setVisibility(ProgressBar.INVISIBLE);
                viewHolder.fullScreenImgView = fullScreenImgView;
                if (tweet.getText() != null) {
                    FirebaseAppIndex.getInstance().update(Tweet.getTweetPageable(tweet));
                    viewHolder.tweetTextView.setText(tweet.getText());
                    viewHolder.tweetTextView.setVisibility(TextView.VISIBLE);
                } else {
                    viewHolder.tweetTextView.setVisibility(TextView.GONE);
                }

                if (tweet.getImageUrl() != null) {

                    viewHolder.tweetImage.setVisibility(ImageView.VISIBLE);
                    String imageUrl = tweet.getImageUrl();
                    try {
                        if (imageUrl.startsWith("gs://")) {
                            StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
                            storageReference.getDownloadUrl().addOnCompleteListener(
                                new OnCompleteListener<Uri>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Uri> task) {
                                        if (task.isSuccessful()) {
                                            String downloadUrl = task.getResult().toString();
                                            Glide.with(viewHolder.tweetImage.getContext())
                                                    .load(downloadUrl)
                                                    .apply(new RequestOptions()
                                                            .fitCenter()
                                                            .format(DecodeFormat.PREFER_ARGB_8888)
                                                            .override(Target.SIZE_ORIGINAL)).into(new SimpleTarget<Drawable>(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL) {
                                                                @Override
                                                                public void onResourceReady(@NonNull final Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                                                    viewHolder.tweetImage.setImageDrawable(resource);
                                                                }
                                                            });
                                        } else {
                                            Log.w(TAG, "Getting download url was not successful.", task.getException());
                                        }
                                    }
                                });
                        } else {
                            Glide.with(viewHolder.tweetImage.getContext()).load(tweet.getImageUrl()).into(viewHolder.tweetImage);
                        }
                    } catch (Exception e) { }
                } else {
                    viewHolder.tweetImage.setVisibility(ImageView.GONE);
                }

                Long millis = tweet.getTime();
                Long diff = System.currentTimeMillis() - millis;
                if (diff > TimeUnit.DAYS.toMillis(1)) {
                    long days = diff / TimeUnit.DAYS.toMillis(1);
                    viewHolder.tweetTime.setText(days + "d");
                } else {
                    Date date = new Date(millis);
                    SimpleDateFormat formatter = new SimpleDateFormat("h:mm a");
                    String formatted = formatter.format(date);
                    viewHolder.tweetTime.setText(formatted);
                }

                viewHolder.setLikesListener(tweet.getId(), tweetsRealtimeRef);
                viewHolder.setCommentsListener(tweet.getId(), tweetsRealtimeRef);
                viewHolder.numComments.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(MainActivity.this, CommentActivity.class);
                        intent.putExtra("KEY", tweet.getId());
                        intent.putExtra("tweetText", tweet.getText());
                        intent.putExtra("tweetImg", tweet.getImageUrl());
                        startActivity(intent);
                    }
                });

                FirebaseUserActions.getInstance().end(Tweet.getViewTweetAction(tweet));
            }
        };

        recyclerAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int tweetCount = recyclerAdapter.getItemCount();
                int lastVisiblePosition = linearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                // System.out.println(lastVisiblePosition);
                // System.out.println(positionStart);
                // System.out.println(tweetCount);
                if (lastVisiblePosition == -1 || (positionStart >= (tweetCount - 1) && lastVisiblePosition == (positionStart - 1))) {
                    tweetRecyclerView.scrollToPosition(positionStart);
                }
                if (lastVisiblePosition == -1 || (positionStart >= (tweetCount - 1))) {
                    //tweetRecyclerView.scrollToPosition(positionStart);
                    //Toast.makeText(MainActivity.this, "New Tweet --> Scroll Up", Toast.LENGTH_SHORT).show();
                }
            }
        });

        tweetRecyclerView.setAdapter(recyclerAdapter);

        tweetBox = findViewById(R.id.tweetTextBox);
        tweetBox.setFilters(new InputFilter[] { new InputFilter.LengthFilter(AppConfig.MAX_TWEET_LENGTH) });
        tweetBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void afterTextChanged(Editable editable) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    tweetButton.setEnabled(true);
                } else {
                    tweetButton.setEnabled(false);
                }
            }
        });

        tweetButton = findViewById(R.id.sendButton);
        tweetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (noLocation) {
                    Toast.makeText(MainActivity.this, "Location Permissions Required.", Toast.LENGTH_LONG).show();
                } else if (!(GeneralTools.isUserInRange(Statics.CURR_LOCATION.getLatitude(), Statics.CURR_LOCATION.getLongitude()))) {
                    Toast.makeText(MainActivity.this, "You must be around Hunter College to tweet.", Toast.LENGTH_LONG).show();
                } else {
                    if (imageSelected && imagePicked != null) {
                        final Uri uri = imagePicked.getUri();
                        Tweet tempMessage = new Tweet(tweetBox.getText().toString(), "Anonymous", "LoadingImageURL", System.currentTimeMillis());
                        tweetsRealtimeRef.push()
                                .setValue(tempMessage, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(DatabaseError databaseError,
                                                           @NonNull DatabaseReference databaseReference) {
                                        if (databaseError == null) {
                                            String key = databaseReference.getKey();
                                            StorageReference storageReference =
                                                    FirebaseStorage.getInstance()
                                                            .getReference(firebaseUser.getUid())
                                                            .child(key)
                                                            .child(uri.getLastPathSegment());

                                            pushImageTweet(storageReference, uri, key);
                                        } else {
                                            Log.w(TAG, "Unable to write message to database.",
                                                    databaseError.toException());
                                        }
                                    }
                                });
                        txtBoxImgView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_add_black_24dp));
                        imageSelected = false;
                        imagePicked = null;
                        tweetBox.setText("");
                    } else {
                        Tweet tweet = new Tweet(tweetBox.getText().toString(),"Anonymous",null, System.currentTimeMillis());
                        tweetsRealtimeRef.push().setValue(tweet);
                        tweetBox.setText("");
                    }
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

        addMediaButton = findViewById(R.id.addMediaButton);
        txtBoxImgView = addMediaButton;
        addMediaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!imageSelected) {
                    PickImageDialog.build(new PickSetup().setVideo(false)).show(MainActivity.this);
                } else {
                    txtBoxImgView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_add_black_24dp));
                    imageSelected = false;
                    imagePicked = null;
                }

            }
        });

        if(!PermissionService.Check_FINE_LOCATION(MainActivity.this)) {
            PermissionService.Request_FINE_LOCATION(MainActivity.this,22);
        }

        fullScreenImgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fullScreenImgView.setVisibility(ImageView.GONE);
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
    }

    private void pushImageTweet(final StorageReference storageReference, Uri uri, final String key) {
        UploadTask uploadTask = storageReference.putFile(uri);
        Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    System.out.println("URI Task Unsuccessful  @ Image Upload");
                }
                // Continue with the task to get the download URL
                return storageReference.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    String download_url = downloadUri.toString();
                    tweetsRealtimeRef.child(key).child("imageUrl").setValue(download_url);
                   //Toast.makeText(MainActivity.this, download_url, Toast.LENGTH_SHORT).show();
                } else {
                    Log.w(TAG, "Image upload task was not successful.", task.getException());
                }
            }
        });

    }

    @Override
    public void onPickResult(PickResult r) {
        if (r.getError() == null) {
            //If you want the Uri.

            //Mandatory to refresh image from Uri.
            //getImageView().setImageURI(null);

            //Setting the real returned image.
            //getImageView().setImageURI(r.getUri());

            //If you want the Bitmap.
            //getImageView().setImageBitmap(r.getBitmap());

            //Image path
            //r.getPath();


            Bitmap scaled = Bitmap.createScaledBitmap(r.getBitmap(), txtBoxImgView.getWidth(), txtBoxImgView.getHeight(), false);
            txtBoxImgView.setImageBitmap(scaled);
            imageSelected = true;
            imagePicked = r;
        } else {
            //Handle possible errors
            //TODO: do what you have to do with r.getError();
            Toast.makeText(this, r.getError().getMessage(), Toast.LENGTH_LONG).show();
        }
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
