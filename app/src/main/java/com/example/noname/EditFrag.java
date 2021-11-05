package com.example.noname;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Cache;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class EditFrag extends Fragment {

    private FirebaseAuth fAuth;
    private StorageReference storage;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private EditText username, oldpassword, newpassword, confirmpassword;
    private Button avatar_btn, submit_btn, back_btn, camera_btn;
    private ImageView avatar;
    private String currentPhotoPath;


    public EditFrag() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_edit, container, false);

        // Declare something needed
        camera_btn = view.findViewById(R.id.edit_camera);
        avatar_btn = view.findViewById(R.id.edit_avatar_btn);
        avatar = view.findViewById(R.id.edit_avatar);

        // Firebase elements
        fAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance().getReference();

        // load the avatar from the firebase
        StorageReference initialavatar = storage.child("users/" + fAuth.getCurrentUser().getUid() + "/avatar.jpg");
        initialavatar.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Picasso.get().load(uri).into(avatar);
            }
        });


        // enable edit avatar function
        avatar_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // evoke the gallery
                Intent evokeGalleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(evokeGalleryIntent, 1000);
            }
        });

        // edit avatar using camera upload
        camera_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(takePictureIntent, 1001);
            }
        });


        // edit part
        username = view.findViewById(R.id.edit_username);
        oldpassword = view.findViewById(R.id.edit_oldpassword);
        newpassword = view.findViewById(R.id.edit_newpassword);
        confirmpassword = view.findViewById(R.id.edit_confirmpassword);


        db.collection("user")
                .whereEqualTo("User ID", fAuth.getCurrentUser().getUid())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            String userString = task.getResult().getDocuments().get(0).get("User Name").toString();
                            username.setText(userString);  // present the current user name on the text-input field
                        } else {
                            Toast.makeText(getActivity(), "Something's Wrong!", Toast.LENGTH_LONG).show();
                        }
                    }
                });


        submit_btn = view.findViewById(R.id.edit_submit_btn);
        submit_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String user = username.getText().toString().trim();
                String old_pwd = oldpassword.getText().toString().trim();
                String new_pwd = newpassword.getText().toString().trim();
                String confirm_pwd = confirmpassword.getText().toString().trim();

                // user name cannot be empty
                if (user.isEmpty()) {
                    username.setError("Username is required!");
                    username.requestFocus();
                    return;
                }


                if (old_pwd.isEmpty()) {
                    // old password is not entered
                    if (new_pwd.length() != 0 || confirm_pwd.length() != 0) {
                        oldpassword.setError("Please make sure fill the required fields to change your password");
                        newpassword.setError("Please make sure fill the required fields to change your password");
                        confirmpassword.setError("Please make sure fill the required fields to change your password");
                        oldpassword.requestFocus();
                        newpassword.requestFocus();
                        confirmpassword.requestFocus();
                    }

                    // only change user name
                    if (new_pwd.isEmpty() && confirm_pwd.isEmpty()) {
                        Map<String, Object> update = new HashMap<>();
                        update.put("User Name", user);

                        db.collection("user")
                                .whereEqualTo("User ID", fAuth.getCurrentUser().getUid())
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                            String documentID = task.getResult().getDocuments().get(0).getId();
                                            db.collection("user")
                                                    .document(documentID)
                                                    .update(update)
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void unused) {
                                                            Toast.makeText(getActivity(), "User name is changed!", Toast.LENGTH_LONG).show();
                                                            getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new ProfileFrag()).commit();
                                                        }
                                                    });
                                        } else {
                                            Toast.makeText(getActivity(), "Something Wrong!", Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });
                    }
                }

                // tend to change the password
                if (!old_pwd.isEmpty()) {
                    AuthCredential crediential = EmailAuthProvider.getCredential(fAuth.getCurrentUser().getEmail(), old_pwd);
                    fAuth.getCurrentUser().reauthenticate(crediential)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    if (new_pwd.isEmpty()) {
                                        newpassword.setError("Please enter your new password!");
                                        newpassword.requestFocus();
                                    }

                                    if (confirm_pwd.isEmpty()) {
                                        confirmpassword.setError("Please confirm your new password!");
                                        confirmpassword.requestFocus();
                                    }

                                    // password should be no less than 7 characters
                                    if (new_pwd.length() < 7) {
                                        newpassword.setError("Password should be longer than 6 chars");
                                        newpassword.requestFocus();
                                    }

                                    // all good
                                    if (!new_pwd.isEmpty() && !confirm_pwd.isEmpty() && new_pwd.length() >= 7) {
                                        // successfully change password
                                        if (new_pwd.equals(confirm_pwd)) {

                                            FirebaseUser fUser = fAuth.getInstance().getCurrentUser();

                                            fUser.updatePassword(new_pwd)
                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            if (task.isSuccessful()) {
                                                                Map<String, Object> update = new HashMap<>();
                                                                update.put("User Name", user);
                                                                db.collection("user")
                                                                        .whereEqualTo("User ID", fAuth.getCurrentUser().getUid())
                                                                        .get()
                                                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                                            @Override
                                                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                                if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                                                                    String documentID = task.getResult().getDocuments().get(0).getId();
                                                                                    db.collection("user")
                                                                                            .document(documentID)
                                                                                            .update(update)
                                                                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                                                @Override
                                                                                                public void onSuccess(Void unused) {
                                                                                                    Toast.makeText(getContext(), "Successfully Changed!", Toast.LENGTH_LONG).show();
                                                                                                    getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new ProfileFrag()).commit();
                                                                                                }
                                                                                            });
                                                                                } else {
                                                                                    Toast.makeText(getActivity(), "Something Wrong!", Toast.LENGTH_LONG).show();
                                                                                }
                                                                            }
                                                                        });

                                                            }
                                                        }
                                                    });
                                        }
                                        // when confirmed password is not matching with the new password entered
                                        else {
                                            confirmpassword.setError("The confirmed password doesn't match with the new password!");
                                            confirmpassword.requestFocus();
                                        }
                                    }
                                }
                            })
                            //  do not pass the authentication
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    oldpassword.setError("The old password doesn't match with the record!");
                                    oldpassword.requestFocus();
                                }
                            });

                }
            }
        });


        // go back part
        back_btn = view.findViewById(R.id.edit_cancel_btn);
        back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new ProfileFrag()).commit();
            }
        });

        return view;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

         if(requestCode==1000) {
             if (resultCode == Activity.RESULT_OK) {

                 Uri avatarUri = data.getData();


                 // upload to firebase
                 StorageReference avatarfile = storage.child("users/" + fAuth.getCurrentUser().getUid() + "/avatar.jpg");
                 avatarfile.putFile(avatarUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                     @Override
                     public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                         avatarfile.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                             @Override
                             public void onSuccess(Uri uri) {
                                 // display the new avatar
                                 Picasso.get().load(uri).into(avatar);
                             }
                         });
                         Toast.makeText(getContext(), "Uploaded Successfully", Toast.LENGTH_LONG).show();
                     }
                 }).addOnFailureListener(new OnFailureListener() {
                     @Override
                     public void onFailure(@NonNull Exception e) {
                         Toast.makeText(getContext(), "Failed", Toast.LENGTH_LONG).show();
                     }
                 });
             }
         }

         if(requestCode==1001) {
             if (resultCode == Activity.RESULT_OK) {
                 Bitmap ava_bit = (Bitmap) data.getExtras().get("data");
                 ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                 ava_bit.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                 byte bb[] = bytes.toByteArray();

                 StorageReference camRef = storage.child("users/" + fAuth.getCurrentUser().getUid() + "/avatar.jpg");
                 camRef.putBytes(bb).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                     @Override
                     public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                         camRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                             @Override
                             public void onSuccess(Uri uri) {
                                 // display the new avatar
                                 Picasso.get().load(uri).into(avatar);
                             }
                         });
                         Toast.makeText(getContext(), "Uploaded Successfully", Toast.LENGTH_LONG).show();
                     }
                 });
             }

         }

    }

}