package com.shubham.chatwithai;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.shubham.chatwithai.databinding.ActivityMainBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttp;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;

    List<MessageModel> messageModelList;
    MessageAdapter messageAdapter;


    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");

    OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        messageModelList = new ArrayList<>();

        messageAdapter = new MessageAdapter(messageModelList);
        binding.recyclerView.setAdapter(messageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        binding.recyclerView.setLayoutManager(llm);

        binding.sendBtn.setOnClickListener(v -> {

            String question = binding.messageEditText.getText().toString().trim();
            addToChat(question, MessageModel.SENT_BY_ME);
            binding.messageEditText.setText("");
            callAPI(question);
            binding.welcomeText.setVisibility(View.GONE);

        });

    }

    void addToChat(String message, String sendBy) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageModelList.add(new MessageModel(message, sendBy));
                messageAdapter.notifyDataSetChanged();
                binding.recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());
            }
        });
    }

    void addResponse(String response) {
        messageModelList.remove(messageModelList.size() - 1);
        addToChat(response, MessageModel.SENT_BY_BOT);

    }

    void callAPI(String question) {
        //okhttp
        messageModelList.add(new MessageModel("Typing...", MessageModel.SENT_BY_BOT));

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", "text-davinci-003");
            jsonBody.put("prompt", question);
            jsonBody.put("max_tokens", 400);
            jsonBody.put("temperature", 0);

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/completions")
                .header("Authorization", "Bearer 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")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                addResponse("Failed to load response due to https..." + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {


                if (response.isSuccessful()) {
                    JSONObject jsonObject ;
                    try {
                        jsonObject = new JSONObject(response.body().string());
                        JSONArray jsonArray = jsonObject.getJSONArray("choices");
                        String result = jsonArray.getJSONObject(0).getString("text");
                        addResponse(result.trim());
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                } else {
                    addResponse("Failed to load response due to" + response.body().string());

                }
            }
        });
    }

}