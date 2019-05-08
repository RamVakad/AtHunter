package com.example.athunter.activity;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import com.example.athunter.R;
import com.example.athunter.global.Statics;
import com.example.athunter.model.Tweet;
import com.example.athunter.global.config.AppConfig;
import com.example.athunter.service.PermissionService;
import com.example.athunter.util.GeneralTools;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.Date;


//TODO: Check GPS Location
//TODO: Media Module: partial + video

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private ProgressBar progressBar;
    private RecyclerView tweetRecyclerView;
    private LinearLayoutManager linearLayoutManager;
    private ImageView addMediaButton;
    private EditText tweetBox;
    private Button tweetButton;
    private boolean noLocation = false;

    private FirebaseUser firebaseUser;
    private DatabaseReference tweetsRealtimeRef;
    private FirebaseRecyclerAdapter<Tweet, TweetPartial> recyclerAdapter;

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
        if(!PermissionService.Check_FINE_LOCATION(MainActivity.this))
        {
            PermissionService.Request_FINE_LOCATION(MainActivity.this,22);
        }

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } catch (SecurityException e) {
            System.out.println("No location permissions.");
            noLocation = true;
        }

        progressBar = findViewById(R.id.progressBar);
        tweetRecyclerView = findViewById(R.id.messageRecyclerView);
        linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        linearLayoutManager.setReverseLayout(true);
        tweetRecyclerView.setLayoutManager(linearLayoutManager);


        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            firebaseAuth.signInAnonymously().addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInAnonymously:success");
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInAnonymously:failure", task.getException());
                        Toast.makeText(MainActivity.this, "Authentication failed.",
                                Toast.LENGTH_LONG).show();
                    }
                }
            });
        }


        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        SnapshotParser<Tweet> parser = new SnapshotParser<Tweet>() {
            @Override
            public Tweet parseSnapshot(DataSnapshot dataSnapshot) {
                Tweet tweet = dataSnapshot.getValue(Tweet.class);
                if (tweet != null) {
                    tweet.setId(dataSnapshot.getKey());
                }
                return tweet;
            }
        };
        tweetsRealtimeRef = databaseReference.child(AppConfig.REALTIME_DB_COLLECTION_NAME);

        FirebaseRecyclerOptions<Tweet> options = new FirebaseRecyclerOptions.Builder<Tweet>().setQuery(tweetsRealtimeRef, parser).build();

        recyclerAdapter = new FirebaseRecyclerAdapter<Tweet, TweetPartial>(options) {
            @NonNull
            @Override
            public TweetPartial onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
                return new TweetPartial(inflater.inflate(R.layout.partial_tweet, viewGroup, false));
            }

            @Override
            protected void onBindViewHolder(@NonNull final TweetPartial viewHolder, int position, @NonNull Tweet tweet) {
                progressBar.setVisibility(ProgressBar.INVISIBLE);
                if (tweet.getText() != null) {
                    FirebaseAppIndex.getInstance()
                            .update(Tweet.getTweetPageable(tweet));
                    viewHolder.tweetTextView.setText(tweet.getText());
                    viewHolder.tweetTextView.setVisibility(TextView.VISIBLE);
                    viewHolder.tweetImage.setVisibility(ImageView.GONE);
                } else if (tweet.getImageUrl() != null) {
                    String imageUrl = tweet.getImageUrl();
                    if (imageUrl.startsWith("gs://")) {
                        StorageReference storageReference = FirebaseStorage.getInstance()
                                .getReferenceFromUrl(imageUrl);
                        storageReference.getDownloadUrl().addOnCompleteListener(
                                new OnCompleteListener<Uri>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Uri> task) {
                                        if (task.isSuccessful()) {
                                            String downloadUrl = task.getResult().toString();
                                            Glide.with(viewHolder.tweetImage.getContext())
                                                    .load(downloadUrl)
                                                    .into(viewHolder.tweetImage);
                                        } else {
                                            Log.w(TAG, "Getting download url was not successful.",
                                                    task.getException());
                                        }
                                    }
                                });
                    } else {
                        Glide.with(viewHolder.tweetImage.getContext())
                                .load(tweet.getImageUrl())
                                .into(viewHolder.tweetImage);
                    }
                    viewHolder.tweetImage.setVisibility(ImageView.VISIBLE);
                    viewHolder.tweetTextView.setVisibility(TextView.GONE);
                }

                Long millis = tweet.getTime();
                Date date = new Date(millis);
                SimpleDateFormat formatter = new SimpleDateFormat("h:mm a");
                String formatted = formatter.format(date);

                viewHolder.tweetTime.setText(formatted);

                FirebaseUserActions.getInstance().end(Tweet.getViewTweetAction(tweet));
            }
        };

        recyclerAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = recyclerAdapter.getItemCount();
                int lastVisiblePosition =
                        linearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (friendlyMessageCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    tweetRecyclerView.scrollToPosition(positionStart);
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
                    Toast.makeText(MainActivity.this, "Location Permissions Required.",
                            Toast.LENGTH_LONG).show();
                } else if (!(GeneralTools.isUserInRange(Statics.CURR_LOCATION.getLatitude(), Statics.CURR_LOCATION.getLongitude()))) {
                    Toast.makeText(MainActivity.this, "You must be around Hunter College to tweet.",
                            Toast.LENGTH_LONG).show();
                } else {
                    Tweet tweet = new
                            Tweet(tweetBox.getText().toString(),
                            "Anonymous",
                            null, System.currentTimeMillis());
                    tweetsRealtimeRef.push().setValue(tweet);
                    tweetBox.setText("");
                }

            }
        });

        addMediaButton = findViewById(R.id.addMediaButton);
        addMediaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                //intent.addCategory(Intent.CATEGORY_OPENABLE);
                //intent.setType("image/*");
                //startActivityForResult(intent, REQUEST_IMAGE);
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == 2) { // 2 = Image
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    final Uri uri = data.getData();
                    Log.d(TAG, "Uri: " + uri.toString());

                    Tweet tempMessage = new Tweet(null, "Anonymous", "LoadingImageURL", System.currentTimeMillis());
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

                                        putImageInStorage(storageReference, uri, key);
                                    } else {
                                        Log.w(TAG, "Unable to write message to database.",
                                                databaseError.toException());
                                    }
                                }
                            });
                }
            }
        }
    }

    private void putImageInStorage(final StorageReference storageReference, Uri uri, final String key) {
        storageReference.putFile(uri).addOnCompleteListener(MainActivity.this,
                new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()) {
                            Tweet tweet =
                                    new Tweet(null, "Anonymous",
                                            storageReference.getDownloadUrl().toString(), System.currentTimeMillis());
                            tweetsRealtimeRef.child(key)
                                    .setValue(tweet);
                        } else {
                            Log.w(TAG, "Image upload task was not successful.",
                                    task.getException());
                        }
                    }
                });
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
