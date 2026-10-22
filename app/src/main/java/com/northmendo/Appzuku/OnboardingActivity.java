package com.northmendo.Appzuku;

import android.os.Bundle;
import android.text.Html;

import com.northmendo.Appzuku.databinding.ActivityOnboardingBinding;

public class OnboardingActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityOnboardingBinding binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setTitle(R.string.onboarding_title);
        binding.onboardingContent.setText(
                Html.fromHtml(getString(R.string.onboarding_body), Html.FROM_HTML_MODE_COMPACT));
        binding.onboardingStart.setOnClickListener(v -> finish());
    }
}
