package com.example.srikkanth.firebasechat;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.Image;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.srikkanth.firebasechat.HelperAdapter.MessageAdapter;
import com.example.srikkanth.firebasechat.HelperClass.ChatMessage;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;
    private UploadTask uploadTask;
    private List<AuthUI.IdpConfig> providers;
    private String mUsername,mPhotoUrl,mMessage;
    private ChildEventListener mChildEventListener;
    private ListView mMessageListView;
    private MessageAdapter mMessageAdapter;
    private RelativeLayout relativeLayout;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private ImageView photoImageView;
    private ImageButton mSendButton;
    private ArrayList<ChatMessage> chatMessages;
    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    private static final int RC_SIGN_IN=1;
    private static final int RC_PHOTO_PICKER =  2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#ed1245")));
        initialize();
        listeners();
        isInternetOn();
    }

    private void listeners() {
        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Fire an intent to show an image picker
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), RC_PHOTO_PICKER);
            }
        });

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                }
                else {
                    mSendButton.setEnabled(false);
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Send messages on click
                if(isInternetOn()) {
                    ChatMessage chatMessage = new ChatMessage(mMessageEditText.getText().toString(), null, mUsername);
                    databaseReference.push().setValue(chatMessage);
                    // Clear input box
                    mMessageEditText.setText("");
                }

            }
        });
        mAuthStateListener=new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                Log.e("Hi","1");
                FirebaseUser user=mAuth.getCurrentUser();
                Log.e("Hi","2");
                if(user!=null){
                    Log.e("Signed in","3");
                    onSignedInInitialize(user.getDisplayName());
                }
                else {
                    Log.e("SignedOut","4");
                    onSignedOutCleanup();
                    startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().setIsSmartLockEnabled(false).setAvailableProviders(providers).setTheme(R.style.LoginTheme).setLogo(R.drawable.logo).build(),RC_SIGN_IN);
                }
            }
        };
        mMessageListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(final AdapterView<?> parent, final View view, final int position, long id) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this,R.style.MyDialogTheme);
                alertDialogBuilder.setMessage("Are you sure,You wanted delete this message?");
                        alertDialogBuilder.setPositiveButton("Yes",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface arg0, int arg1) {
                                        //Toast.makeText(MainActivity.this,"You clicked yes button",Toast.LENGTH_LONG).show();
                                       mMessageAdapter.remove(mMessageAdapter.getItem(position));
                                    }
                                });

                        alertDialogBuilder.setNegativeButton("No",new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Toast.makeText(MainActivity.this,"No",Toast.LENGTH_LONG).show();
                                    }
                                });
                alertDialogBuilder.show();
                return true;
            }
        });
    }

    private void initialize() {
        mUsername = ANONYMOUS;
        // Initialize references to views
        relativeLayout=findViewById(R.id.item_rel_layout);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageListView = (ListView) findViewById(R.id.messageListView);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (ImageButton) findViewById(R.id.sendButton);
        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);
        // Initialize message ListView and its adapter
        chatMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item__message,chatMessages);
        mMessageListView.setAdapter(mMessageAdapter);
        providers= Arrays.asList(new AuthUI.IdpConfig.EmailBuilder().build(),new AuthUI.IdpConfig.GoogleBuilder().build());
        mAuth=FirebaseAuth.getInstance();
        firebaseDatabase=FirebaseDatabase.getInstance();
        databaseReference=firebaseDatabase.getReference("messages");
        mAuth=FirebaseAuth.getInstance();
        firebaseStorage=FirebaseStorage.getInstance();
        storageReference=firebaseStorage.getReference().child("chatPhotos");
    }

    private void attachDatabaseReadListener(){
        if(mChildEventListener==null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    if(dataSnapshot.hasChildren()){
                        ChatMessage chatMessage=dataSnapshot.getValue(ChatMessage.class);
                        mMessageAdapter.add(chatMessage);
                    }
                    else {
                        ChatMessage chatMessage=new ChatMessage();
                        String message=dataSnapshot.getValue(String.class);
                        Log.e("message", message);
                        chatMessage.setMessage(message);
                        chatMessage.setPhotoUrl(null);
                        chatMessage.setUsername("Admin");
                        mMessageAdapter.add(chatMessage);
                    }
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}
                @Override
                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}
                @Override
                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {}
            };
            databaseReference.addChildEventListener(mChildEventListener);
        }
    }
    private void detachDatabaseReadListener() {
        if(mChildEventListener!=null) {
            databaseReference.removeEventListener(mChildEventListener);
            mChildEventListener=null;
        }
    }
    private void onSignedInInitialize(String displayName) {
        mUsername=displayName;
        attachDatabaseReadListener();
    }
    private void onSignedOutCleanup(){
        mUsername=ANONYMOUS;
        mMessageAdapter.clear();
        detachDatabaseReadListener();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id ==R.id.sign_out_menu) {
            AuthUI.getInstance().signOut(this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    //Callback method for firebase Auth UI's startActivity method
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==RC_SIGN_IN){
            if(resultCode==RESULT_OK){
                Toast.makeText(MainActivity.this,"Signed In",Toast.LENGTH_LONG).show();
            }
            else if(resultCode==RESULT_CANCELED){
                Toast.makeText(MainActivity.this,"Cancelled Sign in",Toast.LENGTH_LONG).show();
                finish();
            }
        }
        if(requestCode==RC_PHOTO_PICKER && resultCode==RESULT_OK){
            Uri selectedImageUri=data.getData();
            final StorageReference photoRef=storageReference.child(selectedImageUri.getLastPathSegment());
            uploadTask=photoRef.putFile(selectedImageUri);
            Task<Uri> urlTask=uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    // Continue with the task to get the download URL
                    return photoRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        ChatMessage chatMessage=new ChatMessage(null,downloadUri.toString(),mUsername);
                        databaseReference.push().setValue(chatMessage);
                    } else {
                        Log.e("completed","failed");
                    }
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isInternetOn();
        mAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void finish() {
        super.finish();

        if(mAuthStateListener!=null) {
            mAuth.removeAuthStateListener(mAuthStateListener);
        }
        detachDatabaseReadListener();
        mMessageAdapter.clear();
    }
    private void showSnack(boolean isConnected) {
        String message="";
        int color;
        if (!isConnected) {
            message = "Sorry! Not connected to internet";
            color = Color.RED;
            Snackbar snackbar = Snackbar
                    .make(findViewById(R.id.main_rel), message, Snackbar.LENGTH_LONG);

            View sbView = snackbar.getView();
            TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
            textView.setTextColor(color);
            snackbar.show();
        }
    }
    public final boolean isInternetOn() {

        // get Connectivity Manager object to check connection
        ConnectivityManager connec =
                (ConnectivityManager)getSystemService(getBaseContext().CONNECTIVITY_SERVICE);

        // Check for network connections
        if ( connec.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.CONNECTED ||
                connec.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.CONNECTING ||
                connec.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.CONNECTING ||
                connec.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.CONNECTED ) {

            // if connected with internet

            showSnack(true);
            return true;

        } else if (
                connec.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.DISCONNECTED ||
                        connec.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.DISCONNECTED  ) {
            showSnack(false);
            return false;
        }
        return false;
    }
}
