package com.example.finalprojectvegan;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.finalprojectvegan.Adapter.MyFeedAdapter;
import com.example.finalprojectvegan.Model.FeedInfo;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.Date;

public class FragMyFeed extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    TextView Tv_MyFeed_UerId, Tv_MyFeed_VeganType, Tv_MyFeed_Allergy, Tv_Notice;
    ImageView Iv_MyFeedProfile;

    FirebaseUser firebaseUser;
    FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
    FirebaseFirestore db;

    String USER_ID;
    String USER_VEGAN_TYPE;
    String USER_ALLERGY;
    String Uid;

    public FragMyFeed() {
        // Required empty public constructor
    }

    public static FragMyFeed newInstance(String param1, String param2) {
        FragMyFeed fragment = new FragMyFeed();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public static FragMyFeed newInstance() {
        return new FragMyFeed();
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
        View view =  inflater.inflate(R.layout.fragment_frag_my_feed, container, false);

        Tv_Notice = view.findViewById(R.id.Tv_Notice);
        Iv_MyFeedProfile = view.findViewById(R.id.Iv_MyFeedProfile);
        Tv_MyFeed_UerId = view.findViewById(R.id.Tv_MyFeed_UerId);
        Tv_MyFeed_VeganType = view.findViewById(R.id.Tv_MyFeed_VeganType);
        Tv_MyFeed_Allergy = view.findViewById(R.id.Tv_MyFeed_Allergy);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

//        getFirebaseProfileImage(firebaseUser);

        db = FirebaseFirestore.getInstance();
        db.collection("users")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {

                            if (firebaseUser != null) {

                                Uid = firebaseUser.getUid();

                                for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                                    Log.d("MYFEED", "SUCCESS");

                                    if (documentSnapshot.getId().equals(Uid)) {
                                        USER_ID = documentSnapshot.getData().get("userId").toString();
                                        USER_VEGAN_TYPE = documentSnapshot.getData().get("userVeganType").toString();
                                        USER_ALLERGY = documentSnapshot.getData().get("userAllergy").toString().replaceAll("\\[|\\]", "").replaceAll(", ",", ");
                                        Tv_MyFeed_UerId.setText(USER_ID);
                                        Tv_MyFeed_VeganType.setText(USER_VEGAN_TYPE);
                                        Tv_MyFeed_Allergy.setText(USER_ALLERGY);
                                    }
                                }
                            }
                        } else {
                            Log.d("MYFEED", "ERROR", task.getException());
                        }
                    }
                });

        db.collection("posts")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {

                            ArrayList<FeedInfo> MyFeedList = new ArrayList<>();

                            if (firebaseUser != null) {

                            for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                                Log.d("MYFEED POST", documentSnapshot.getId() + " => " + documentSnapshot.getData());
                                MyFeedList.add(new FeedInfo(
                                        documentSnapshot.getData().get("title").toString(),
                                        documentSnapshot.getData().get("content").toString(),
                                        documentSnapshot.getData().get("publisher").toString(),
                                        documentSnapshot.getId(),
                                        documentSnapshot.getData().get("uri").toString(),
                                        new Date(documentSnapshot.getDate("createdAt").getTime())));

                            }

                                Tv_Notice.setVisibility(GONE);

                                RecyclerView recyclerView = view.findViewById(R.id.Rv_MyFeed);
                                recyclerView.setHasFixedSize(true);
                                recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

                                RecyclerView.Adapter mAdapter = new MyFeedAdapter(getActivity(), MyFeedList);
                                recyclerView.setAdapter(mAdapter);

                            }

                            Tv_Notice.setVisibility(VISIBLE);

                        } else {
                            Log.d("error", "Error getting documents", task.getException());
                        }
                    }
                });

        return view;
    }

//    public void loadImage() {
//
//        StorageReference storageReference = firebaseStorage.getReference();
//        StorageReference pathReference = storageReference.child("users");
//        if (pathReference == null) {
//            Toast.makeText(getActivity(), "저장소에 사진이 없습니다.", Toast.LENGTH_SHORT).show();
//        } else {
//            StorageReference submitProfile = storageReference.child("users/" + firebaseUser.getUid() + "/profileImage.jpg");
//            submitProfile.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
//                @Override
//                public void onSuccess(Uri uri) {
//                    Glide.with(getActivity()).load(uri).centerCrop().override(300).into(imageView_profile);
//                }
//            }).addOnFailureListener(new OnFailureListener() {
//                @Override
//                public void onFailure(@NonNull Exception e) {
//
//                }
//            });
//        }
//    }
//
//    private void getFirebaseProfileImage(FirebaseUser id) {
//        File file = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES + "/profileImage");
//        if (!file.isDirectory()) {
//            file.mkdir();
//        }
//        loadImage();
//    }
}