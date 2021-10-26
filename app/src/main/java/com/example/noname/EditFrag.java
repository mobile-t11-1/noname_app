package com.example.noname;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
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
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link EditFrag#newInstance} factory method to
 * create an instance of this fragment.
 */
public class EditFrag extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private FirebaseAuth fAuth;
    private StorageReference storage;
    private FirebaseFirestore db =  FirebaseFirestore.getInstance();

    private EditText username, oldpassword, newpassword, confirmpassword;
    private Button avatar_btn, submit_btn, back_btn;
    private ImageView avatar;





    public EditFrag() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment EditFrag.
     */
    // TODO: Rename and change types and number of parameters
    public static EditFrag newInstance(String param1, String param2) {
        EditFrag fragment = new EditFrag();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_edit, container, false);

        // Declare something needed
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
                        if (task.isSuccessful() && !task.getResult().isEmpty()){
                            String userString = task.getResult().getDocuments().get(0).get("User Name").toString();
                            username.setText(userString);  // present the current user name on the text-input field
                        }
                        else{
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

                if(user.isEmpty()){
                    username.setError("Username is required!");
                    username.requestFocus();
                    return;
                }

                if(old_pwd.isEmpty()){
                    if(new_pwd.length()!=0 || confirm_pwd.length()!=0){
                        oldpassword.setError("Please make sure fill the required fields to change your password");
                        newpassword.setError("Please make sure fill the required fields to change your password");
                        confirmpassword.setError("Please make sure fill the required fields to change your password");
                        oldpassword.requestFocus();
                        newpassword.requestFocus();
                        confirmpassword.requestFocus();
                    }

                    // only change user name
                    if(new_pwd.isEmpty() && confirm_pwd.isEmpty()){
                        Map<String, Object> update = new HashMap<>();
                        update.put("User Name", user);

                        db.collection("user")
                                .whereEqualTo("User ID", fAuth.getCurrentUser().getUid())
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        if (task.isSuccessful() && !task.getResult().isEmpty()){
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
                                        }

                                        else{
                                            Toast.makeText(getActivity(), "Something Wrong!", Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });
                    }
                }

                if(!old_pwd.isEmpty()){
                    AuthCredential crediential = EmailAuthProvider.getCredential(fAuth.getCurrentUser().getEmail(), old_pwd);
                    fAuth.getCurrentUser().reauthenticate(crediential)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    if(new_pwd.isEmpty()){
                                        newpassword.setError("Please enter your new password!");
                                        newpassword.requestFocus();
                                    }

                                    if(confirm_pwd.isEmpty()){
                                        confirmpassword.setError("Please confirm your new password!");
                                        confirmpassword.requestFocus();
                                    }

                                    if(new_pwd.length() < 7){
                                        newpassword.setError("Password should be longer than 6 chars");
                                        newpassword.requestFocus();
                                    }

                                    if(!new_pwd.isEmpty() && !confirm_pwd.isEmpty() && new_pwd.length() >= 7){
                                        // successfully change password
                                        if(new_pwd.equals(confirm_pwd)){

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
                                                                                if (task.isSuccessful() && !task.getResult().isEmpty()){
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
                                                                                }

                                                                                else{
                                                                                    Toast.makeText(getActivity(), "Something Wrong!", Toast.LENGTH_LONG).show();
                                                                                }
                                                                            }
                                                                        });

                                                            }
                                                        }
                                                    });
                                        }

                                        else{
                                            confirmpassword.setError("The confirmed password doesn't match with the new password!");
                                            confirmpassword.requestFocus();
                                        }
                                    }
                                }
                            })
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
        switch(requestCode){
            case 1000:
                if(resultCode == Activity.RESULT_OK){

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

                break;
        }
    }
}