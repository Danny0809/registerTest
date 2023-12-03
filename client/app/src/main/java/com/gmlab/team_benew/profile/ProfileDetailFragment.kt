package com.gmlab.team_benew.profile

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.gmlab.team_benew.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ProfileDetailFragment: Fragment() {

    private lateinit var cv_bottom : CardView
    private lateinit var imgb_picture: ImageButton
    private lateinit var et_nickname: EditText
    private lateinit var et_email: EditText
    private lateinit var et_introduce: EditText
    private lateinit var btn_modify: Button
    private lateinit var tv_gender : TextView
    private lateinit var tv_birthday : TextView
    private lateinit var spn_experience : Spinner
    private lateinit var et_role : EditText
    private lateinit var et_link : EditText
    private lateinit var tv_phone : TextView
    private lateinit var tv_major : TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile_detail, container, false)

        val sharedPref = context?.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)

        val token = sharedPref?.getString("userToken", "")
        val account = sharedPref?.getString("userAccount", "")
        val memberId = sharedPref?.getInt("loginId", 0)

        cv_bottom = view.findViewById(R.id.cv_profilecardDetail_bottom)
        imgb_picture = view.findViewById(R.id.imgb_profilecardDetail_picture)
        et_nickname = view.findViewById(R.id.et_profilecardDetail_nickname)
        et_email = view.findViewById(R.id.et_profilecardDetail_email)
        et_introduce = view.findViewById(R.id.et_profilecardDetail_introduce)
        btn_modify = view.findViewById(R.id.btn_profilecardDetail_modify)
        tv_gender = view.findViewById(R.id.tv_profilecardDetail_gender)
        tv_birthday = view.findViewById(R.id.tv_profilecardDetail_birthday)
        spn_experience = view.findViewById(R.id.spn_profilecardDetail_projectExperience)
        et_role = view.findViewById(R.id.et_profilecardDetail_role)
        et_link = view.findViewById(R.id.et_profilecardDetail_link)
        tv_phone = view.findViewById(R.id.tv_profilecardDetail_phone)
        tv_major = view.findViewById(R.id.tv_profilecardDetail_major)

        imgb_picture.clipToOutline = true
        imgb_picture.visibility = View.VISIBLE

        disableInputFields()

        if (memberId != null) {
            if (token != null) {
                getProfileToServer(token, memberId)
            }
        }

        imgb_picture.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(intent, 1)
        }

        btn_modify.setOnClickListener {
            if (btn_modify.text.toString() == "수정") {
                enableInputFields()

                btn_modify.text = "저장"
            } else {
                if (token != null) {
                    if (memberId != null) {
                        postProfileToServer(token, memberId)
                    }
                }

                disableInputFields()


                btn_modify.text = "수정"
            }
        }

        cv_bottom.setOnClickListener {
            handleLinkClick()
        }

        return view
    }

    private fun handleLinkClick(){
        val link = et_link.text.toString()

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        startActivity(intent)
    }

    private fun postProfileToServer(token : String, memberId : Int){

        var instruction = et_introduce.text.toString()
        var nickname = et_nickname.text.toString()
        var role = et_role.text.toString()
        var personalLink = et_link.text.toString()

//      스피너 처리
        var projectExperience_value = spn_experience.selectedItem.toString()
        var projectExperience : Boolean
        if(projectExperience_value == "유")
        {
            projectExperience = true
        }
        else {
            projectExperience = false
        }

        if(memberId == null){
            return
        }

        val retrofit = Retrofit.Builder()
            .baseUrl("http://ec2-3-39-251-72.ap-northeast-2.compute.amazonaws.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(postProfileDetailRequest::class.java)

        val request = postProfileDetailData(instruction, nickname, projectExperience, role, personalLink)

        val call: Call<Boolean> = apiService.postProfile("Bearer $token", memberId, request)

        call.enqueue(object : Callback<Boolean> {
            override fun onResponse(call: Call<Boolean>, response: Response<Boolean>) {
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "저장했습니다", Toast.LENGTH_LONG).show();
                } else{
                    Toast.makeText(requireContext(), "저장 실패", Toast.LENGTH_LONG).show();
                }
            }

            override fun onFailure(call: Call<Boolean>, t: Throwable) {
                Log.e("API_CALL_FAILURE", "API Call Failed", t)
            }

        })
    }

    private fun getProfileToServer(token : String, memberId : Int){
        if(memberId == null){
            return
        }

        val retrofit = Retrofit.Builder()
            .baseUrl("http://ec2-3-39-251-72.ap-northeast-2.compute.amazonaws.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(getProfileDetailRequest::class.java)

        val call: Call<getProfileDetailData> = apiService.getProfile("Bearer $token", memberId)

        call?.enqueue(object : Callback<getProfileDetailData> {
            override fun onResponse(call: Call<getProfileDetailData>, response: Response<getProfileDetailData>) {
                if (response.isSuccessful) {
                    val profileData: getProfileDetailData? = response.body()
                    profileData?.let {
                        val nickname = it.nickname ?: ""
                        val instruction = it.instruction ?: ""
                        val personalLink = it.personalLink ?: ""
                        val projectExperience = it.projectExperience ?: false
                        val role = it.role ?: ""

                        val birthday = it.member?.birthday ?: ""
                        val email = it.member?.email ?: ""
                        val gender = it.member?.gender ?: ""
                        val major = it.member?.major ?: ""
                        val phoneNumber = it.member?.phoneNumber ?: ""

                        //UI에 저장()

                        et_nickname.setText(nickname)
                        et_email.setText(email)
                        et_introduce.setText(instruction)
                        tv_gender.text = gender
                        tv_birthday.text = birthday
                        et_role.setText(role)
                        et_link.setText(personalLink)
                        tv_phone.text = phoneNumber
                        tv_major.text = major

                        if(projectExperience == true)
                        {
                            spn_experience.setSelection(0)
                        }
                        else
                        {
                            spn_experience.setSelection(1)
                        }

                    }
                }
                else {
                }
            }

            override fun onFailure(call: Call<getProfileDetailData>, t: Throwable) {

            }
        })

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 1 && resultCode == AppCompatActivity.RESULT_OK && data != null){
            val selectedImage : Uri? = data.data
            imgb_picture.setImageURI(selectedImage)
        }
    }

    private fun disableInputFields() {
        val originalNicknameTextColor = et_nickname.currentTextColor
        val originalEmailTextColor = et_email.currentTextColor
        val originalIntroduceTextColor = et_introduce.currentTextColor
        val originalLinkTextColor = et_link.currentTextColor
        val originalRoleTextColor = et_role.currentTextColor

        imgb_picture.isEnabled = false
        et_nickname.isEnabled = false
        et_email.isEnabled = false
        et_introduce.isEnabled = false
        spn_experience.isEnabled = false
        et_role.isEnabled = false
        et_link.isEnabled = false

        et_nickname.setTextColor(originalNicknameTextColor)
        et_email.setTextColor(originalEmailTextColor)
        et_introduce.setTextColor(originalIntroduceTextColor)
        et_link.setTextColor(originalLinkTextColor)
        et_role.setTextColor(originalRoleTextColor)

        cv_bottom.isEnabled = true
    }

    private fun enableInputFields() {
        imgb_picture.isEnabled = true
        et_nickname.isEnabled = true
        et_introduce.isEnabled = true
        spn_experience.isEnabled = true
        et_role.isEnabled = true
        et_link.isEnabled = true
        et_email.isEnabled = true

        cv_bottom.isEnabled = false
    }
}