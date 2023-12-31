package com.cchaegog.chaegog;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cchaegog.chaegog.Model.UserVeganAllergyInfo;
import com.cchaegog.chaegog.Model.UserVeganTypeInfo;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;
import com.lakue.lakuepopupactivity.PopupActivity;
import com.lakue.lakuepopupactivity.PopupGravity;
import com.lakue.lakuepopupactivity.PopupResult;
import com.lakue.lakuepopupactivity.PopupType;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class OcrActivity extends AppCompatActivity {
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    final private static String TAG = "tag";
    private static final int CAMERA = 100;
    private static final int GALLERY = 101;

    Bitmap bitmap;
    Button cameraBtn;
    Button galleryBtn;
    Button goOcr;
    ImageView ocrImage;
    Intent intent;
    InputImage image;
    TextView ocrTextView;

    // visibility=gone 상태인것
    TextView n_ingredient_text;
    TextView n_ingredient;
    TextView allergy_text;
    TextView allergy_ingredient;
    TextView recomm_text;
    TextView recomm_textView;
    TextView y_ingredient_text;
    ImageView recomm_image; // 추후 리사이클러뷰로 구현해야함.
    public String resultText;
    String OcrFoodStr;
    String OcrResultStr;
    boolean checkFit; // flag 변수
    String USER_ID; // 사용자 닉네임
    String USER_VEGAN_TYPE; // 사용자 채식주의 유형
    String USER_VEGAN_ALLERGY; // 사용자 알러지 타입

    ActivityResultLauncher<String> mGetContent;
    List<String> listFoodName = new ArrayList<>();

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    @RequiresApi(api = Build.VERSION_CODES.M)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr);

        ocrImage = findViewById(R.id.ocrimagepopup);
//        cameraBtn = findViewById(R.id.cameraBtn);
//        galleryBtn = findViewById(R.id.galleryBtn);
        goOcr = findViewById(R.id.goOcr);

        n_ingredient_text = findViewById(R.id.n_ingredient_text);
        y_ingredient_text = findViewById(R.id.y_ingredient_text);
        n_ingredient = findViewById(R.id.n_ingredient);
        allergy_text = findViewById(R.id.allergy_text);
        allergy_ingredient = findViewById(R.id.allergy_ingredient);
        recomm_text = findViewById(R.id.recomm_text);
        recomm_textView = findViewById(R.id.recomm_textView);
//        recomm_image = findViewById(R.id.recomm_image);

        TextRecognizer recognizer = TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());

        Intent intent = new Intent(getBaseContext(), PopupActivity.class);
        intent.putExtra("type", PopupType.SELECT);
        intent.putExtra("gravity", PopupGravity.CENTER);
        intent.putExtra("title", "사진을 불러올 기능을 선택하세요");
        intent.putExtra("buttonLeft", "카메라");
        intent.putExtra("buttonRight", "갤러리");
        startActivityForResult(intent, 2);

        // 카메라 권한 설정
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "권한 설정 완료");
            } else {
                Log.d(TAG, "권한 설정 요청");
                ActivityCompat.requestPermissions(OcrActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }

        // ocr 스캔하기 버튼 클릭시
        goOcr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // ImageView에 이미지가 있으면
                if(ocrImage.getDrawable() != null){
                    // 텍스트 인식 함수 실행
                    TextRecognition(recognizer);
                } else {
                    Toast.makeText(OcrActivity.this, "이미지를 넣어주세요.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 사용자 id, 채식 유형, 알러지 타입 가져오기
        db.collection("users")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {

                            ArrayList<UserVeganTypeInfo> postUserList = new ArrayList<>();
                            ArrayList<UserVeganAllergyInfo> postUserList2 = new ArrayList<>();

                            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                            if (firebaseUser != null) {

                                String uid = firebaseUser.getUid();

                                for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                                    Log.d("success", documentSnapshot.getId() + " => " + documentSnapshot.getData());
                                    postUserList.add(new UserVeganTypeInfo(
                                            documentSnapshot.getData().get("userVeganType").toString()));
                                    postUserList2.add(new UserVeganAllergyInfo(
                                            documentSnapshot.getData().get("userAllergy").toString()));
//                                            documentSnapshot.getData().get("similarAllergy").toString()));

                                    if (documentSnapshot.getId().equals(uid)) {
                                        USER_ID = documentSnapshot.getData().get("userId").toString();
                                        USER_VEGAN_TYPE = documentSnapshot.getData().get("userVeganType").toString();
                                        USER_VEGAN_ALLERGY = documentSnapshot.getData().get("userAllergy").toString();
                                    }
                                }
                            }
                        } else {
                            Log.d("error", "Error getting documents", task.getException());
                        }
                    }
                });

        mGetContent=registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri result) {
                Intent intent1 = new Intent(OcrActivity.this, CropperActivity.class);
                intent1.putExtra("DATA", result.toString());
                startActivityForResult(intent1, 101);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            //데이터 받기
            if (requestCode == 2) {
                PopupResult result = (PopupResult) data.getSerializableExtra("result");
                if (result == PopupResult.LEFT) {
                    // 카메라
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    activityResultPicture.launch(intent);

                } else if (result == PopupResult.RIGHT) {
                    // 갤러리
                    mGetContent.launch("image/*");
                }
            }
        } else if(resultCode == -2 && requestCode == 101){
            Log.d("resultcode", resultCode + "");
            String result = data.getStringExtra("RESULT");
            Uri resultUri = null;
            if(result != null){
                resultUri = Uri.parse(result);
                Log.d("RESULT", result);
            }
            setImage(resultUri);

        }
    }
    // 카메라 실행시
    ActivityResultLauncher<Intent> activityResultPicture = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        bitmap = (Bitmap) extras.get("data");
                        ocrImage.setImageBitmap(bitmap);

                        image = InputImage.fromBitmap(bitmap, 0);

//                        Intent intent1 = new Intent(OcrActivity.this, CropperActivity.class);
//                        intent1.putExtra("DATA", result.toString());
//                        startActivityForResult(intent1, 101);
                    }
                }
            }
    );


    // 갤러리 이미지 이미지뷰에
    private void setImage(Uri uri) {
        try{
            InputStream in = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(in);
            ocrImage.setImageBitmap(bitmap);

            image = InputImage.fromBitmap(bitmap, 0);

            Log.e("setImage", "이미지 to 비트맵");
        } catch (FileNotFoundException e){
            e.printStackTrace();
        }
    }

    // 텍스트 인식 실행 결과
    private void TextRecognition(TextRecognizer recognizer) {
        Task<Text> result = recognizer.process(image)
                // 이미지 인식에 성공하면 실행되는 리스너
                .addOnSuccessListener(new OnSuccessListener<Text>() {
                    @Override
                    public void onSuccess(Text visionText) {
                        Log.e("텍스트 인식", "성공");
                        resultText = visionText.getText();

                        // DB 원재료명 리스트 - 비건 타입에 따라 가져오기
                        mAuth = FirebaseAuth.getInstance();
                        mDatabase = FirebaseDatabase.getInstance().getReference("Foods");
                        mDatabase.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                for(DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                    String foodGroup = snapshot.child("FoodGroup").getValue().toString();
                                    switch (USER_VEGAN_TYPE) {
                                        case "비건":
                                            if(foodGroup.equals("육류 및 그 제품") || foodGroup.equals("어패류 및 그 제품") || foodGroup.equals("우유 및 그 제품") || foodGroup.equals("난류")) {
                                                listFoodName.add(snapshot.child("FoodName").getValue().toString());
                                            }
                                            break;
                                        case "락토" :
                                            if(foodGroup.equals("육류 및 그 제품") || foodGroup.equals("어패류 및 그 제품") || foodGroup.equals("난류")) {
                                                listFoodName.add(snapshot.child("FoodName").getValue().toString());
                                            }
                                            break;
                                        case "오보":
                                            if(foodGroup.equals("육류 및 그 제품") || foodGroup.equals("어패류 및 그 제품") || foodGroup.equals("우유 및 그 제품")) {
                                                listFoodName.add(snapshot.child("FoodName").getValue().toString());
                                            }
                                            break;
                                        case "락토오보":
                                            if(foodGroup.equals("육류 및 그 제품") || foodGroup.equals("어패류 및 그 제품")) {
                                                listFoodName.add(snapshot.child("FoodName").getValue().toString());
                                            }
                                            break;
                                        case "페스코":
                                            if(snapshot.child("FoodGroup").getValue().toString().equals("육류 및 그 제품")) {
                                                listFoodName.add(snapshot.child("FoodName").getValue().toString());
                                            }
                                            break;
                                        case "폴로":
                                            String foodInfoName = "소고기, 양고기, 돼지고기, 한우, 삼겹살, 등심, 목살, 베이컨";
                                            String PolloArray[] = foodInfoName.split(",");
                                            break;
                                    }

                                }
                                compare();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.w("TAG", "Failed to read value.", error.toException());
                            }
                        });
                    }
                })
                // 이미지 인식에 실패하면 실행되는 리스너
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e("텍스트 인식", "실패: " + e.getMessage());
                            }
                        });
    }

    // 팝업창
    public void getAlertDialog(String header, String content, String ok, String no, String normal) {

        //TODO 타이틀 및 내용 표시
        final String Tittle = header;
        final String Message = content;

        //TODO 버튼 이름 정의
        String buttonNo = no;
        String buttonYes = ok;
        String buttonNature = normal;

        //TODO AlertDialog 팝업창 생성
        new AlertDialog.Builder(OcrActivity.this)
                .setTitle(Tittle) //[팝업창 타이틀 지정]
                //.setIcon(R.drawable.tk_app_icon) //[팝업창 아이콘 지정]
                .setMessage(Message) //[팝업창 내용 지정]
                .setCancelable(false) //[외부 레이아웃 클릭시도 팝업창이 사라지지않게 설정]
                .setPositiveButton(buttonYes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                    }
                })
                .setNegativeButton(buttonNo, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                    }
                })
                .setNeutralButton(buttonNature, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                    }
                })
                .show();
    }

    // OCR 판단
    public void compare(){
        checkFit = true;
        ocrTextView = findViewById(R.id.ocrTextView);

        List<String> listFoodName2 = new ArrayList<>();
        List<String> OcrResultList = new ArrayList<>();

        List<String> listUserAllergy = new ArrayList<>();
        List<String> listOcrResult = new ArrayList<>();

        if(resultText != null){
            for(int i=0; i < listFoodName.size(); i++) {
                String foodNameArr[] = listFoodName.get(i).split(",");
                String resultArr[] = resultText.split(",");
                int foodNameSize = foodNameArr.length;
                int resultSize = resultArr.length;

                // 알러지
                for(int j=0; j < resultSize; j++) {
                    OcrResultStr = resultArr[j].trim();
                    String allergyArr[] = USER_VEGAN_ALLERGY.split(" ");
                    USER_VEGAN_ALLERGY = USER_VEGAN_ALLERGY.trim();
                    String allergy;
                    for (int k = 0; k < allergyArr.length; k++) {
                        allergy = allergyArr[k].trim();
                        if (OcrResultStr.matches("(.*)" + USER_VEGAN_ALLERGY + "(.*)")) {
                            listUserAllergy.add(allergy);
                            listOcrResult.add(OcrResultStr);
                            checkFit = false;
                        }
                    }
                }

                for (int j = 0; j < resultSize; j++) {
                    OcrResultStr = resultArr[j].trim(); // ocr 스캔한 텍스트 배열을 OcrResultStr에 넣음
                    for (int k = 0; k < foodNameSize; k++) {
                        OcrFoodStr = foodNameArr[k].trim(); // db에서 가져온 원재료명을 OcrFoodStr에 넣음
                        if (OcrResultStr.equals(OcrFoodStr)) { // 추출한 텍스트에 db에 있는 원재료명이 있다면
                            listFoodName2.add(OcrFoodStr); // OcrFoodStr(db)을 listFoodName2에 넣음
                            OcrResultList.add(OcrResultStr); // OcrResultStr을 OcrResultList에 넣음
                            listFoodName2.retainAll(OcrResultList); // listFoodName2와 OcrResultList의 공통요소만 남기고 제거 -> 결과 화면에서 부적합 원재료명 텍스트뷰에 넣어서 보여줌
                            checkFit = false;
                        }
                    }
                }
            }
        } else {
            checkFit = true;
        }

        List<String> newList = new ArrayList<String>();
        for(String strValue : listFoodName2) {
            if(!newList.contains(strValue)) {
                newList.add(strValue);
            }
        }
        String n_ingre1 = newList.toString().replace("[","").replace("]","");

        List<String> newAllergyList = new ArrayList<String>();
        for(String strValue : listOcrResult) {
            if(!newAllergyList.contains(strValue)) {
                newAllergyList.add(strValue);
            }
        }
        String n_ingre2 = newAllergyList.toString().replace("[","").replace("]","");
        Log.d("OCRTEST", "부적합/알러지 유발 원재료명 : " + n_ingre1 + "/" + n_ingre2);

        if(!checkFit){
            Log.d("OCRTEST", resultText + " - 채식유형 및 알러지에 부적합합니다.");
            ocrTextView.setText(USER_ID + "님에게 맞지않는 제품입니다.");
            ocrTextView.setTextSize(20);
            n_ingredient.setText(n_ingre1);
            allergy_ingredient.setText(n_ingre2);
            View ocrLayout = findViewById(R.id.ocrLayout);
            ocrLayout.setBackgroundColor(Color.parseColor("#FFF8E1"));

            // 숨기기
            ocrImage.setVisibility(View.GONE);
            goOcr.setVisibility(View.GONE);

            // 보여주기
            n_ingredient_text.setVisibility(View.VISIBLE);
            n_ingredient.setVisibility(View.VISIBLE);
            allergy_text.setVisibility(View.VISIBLE);
            allergy_ingredient.setVisibility(View.VISIBLE);
            recomm_textView.setVisibility(View.VISIBLE);
            recomm_text.setVisibility(View.VISIBLE);
            //recomm_image.setVisibility(View.VISIBLE);

        }else{
            Log.d("OCRTEST", resultText + " - 채식유형 및 알러지에 적합합니다.");
            y_ingredient_text.setText(USER_ID + "님에게\n적합한 제품입니다.");
            y_ingredient_text.setVisibility(View.VISIBLE);

            // 숨기기
            ocrTextView.setVisibility(View.GONE);
            ocrImage.setVisibility(View.GONE);
            goOcr.setVisibility(View.GONE);

        }
    }
}