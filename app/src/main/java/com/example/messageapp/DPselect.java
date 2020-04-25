package com.example.messageapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.messageapp.Model.User;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class DPselect extends AppCompatActivity {
    private Button btn_skip;

    private CircleImageView image_profile;

    private static final int IMAGE_REQUEST = 1;

    private FirebaseUser fUser;
    private DatabaseReference reference;
    private StorageReference storageReference;

    private Uri imageUri;

    private StorageTask uploadTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dpselect);

        //bind views
        btn_skip = findViewById(R.id.btn_skip);
        image_profile = findViewById(R.id.profile_image);


        //getting current user
        fUser = FirebaseAuth.getInstance().getCurrentUser();
        storageReference = FirebaseStorage.getInstance().getReference("uploads");

        reference = FirebaseDatabase.getInstance().getReference("Users").child(fUser.getUid());
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //getting the datasnapshot
                User user = dataSnapshot.getValue(User.class);
                if (user.getImageURL().equals("default")) {

                } else {

                    Glide.with(DPselect.this).load(user.getImageURL()).into(image_profile);
                    Intent intent=new Intent(DPselect.this,MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        image_profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImage();

            }
        });
        btn_skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(DPselect.this,MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });


    }

    private void openImage() {
        Intent intent = new Intent();
        //set type to image/* this ensures that only image file is selected
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        //IMAGE_REQUEST is the request code that is passed into the startActivityForResult
        startActivityForResult(intent, IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            if (uploadTask != null && uploadTask.isInProgress()) {
                Toast.makeText(DPselect.this, "Upload in progress", Toast.LENGTH_SHORT).show();
            } else {
                uploadImage();
            }

        }
    }

    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = DPselect.this.getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }
    private void uploadImage(){
        final ProgressDialog pd=new ProgressDialog(DPselect.this);
        pd.setMessage("Uploading");
        pd.show();
        if(imageUri!=null){
            final StorageReference mstorageReference=storageReference.child(System.currentTimeMillis()
                    +"."+getFileExtension(imageUri));
            uploadTask=mstorageReference.putFile(imageUri);
            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot,Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if(!task.isSuccessful()){
                        throw task.getException();
                    }
                    return mstorageReference.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if(task.isSuccessful()){
                        Uri downloadUri=task.getResult();
                        String mUri=downloadUri.toString();

                        reference=FirebaseDatabase.getInstance().getReference("Users").child(fUser.getUid());
                        HashMap<String,Object> map=new HashMap<>();
                        map.put("imageURL",""+mUri);
                        reference.updateChildren(map);
                        pd.dismiss();

                    }else{
                        Toast.makeText(DPselect.this, "Failed1", Toast.LENGTH_SHORT).show();
                        pd.dismiss();
                    }

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(DPselect.this, e.getMessage(),Toast.LENGTH_SHORT).show();
                    pd.dismiss();
                }
            });
        }else{
            Toast.makeText(DPselect.this, "No Image selected", Toast.LENGTH_SHORT).show();
        }

    }
}
