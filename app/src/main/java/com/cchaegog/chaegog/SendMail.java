package com.cchaegog.chaegog;

import android.content.Context;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import javax.mail.MessagingException;
import javax.mail.SendFailedException;

public class SendMail extends AppCompatActivity {
    String user = "cchaegog@gmail.com"; // 보내는 계정의 id
    String password = "gqxg poqe mstf ekye"; // 보내는 계정의 pw

    String uidKey; // 신고당한 게시물 ID


//    GMailSender gMailSender = new GMailSender(user, password);
//    String emailCode = gMailSender.getEmailCode();

    public void sendSecurityCode(Context context, String sendTo, String postId) {

        try {
            GMailSender gMailSender = new GMailSender(user, password);
            gMailSender.sendMail("[ " + postId + " ]" + "게시물 신고 보고서", "3회 이상 신고되었습니다.", sendTo);
            Toast.makeText(context, "이메일을 성공적으로 보냈습니다.", Toast.LENGTH_SHORT).show();
        } catch (SendFailedException e) {
            Toast.makeText(context, "이메일 형식이 잘못되었습니다.", Toast.LENGTH_SHORT).show();
        } catch (MessagingException e) {
            Toast.makeText(context, "인터넷 연결을 확인해주십시오", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}