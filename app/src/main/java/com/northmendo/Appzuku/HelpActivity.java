package com.northmendo.Appzuku;

import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;

import com.northmendo.Appzuku.databinding.ActivityHelpBinding;

public class HelpActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityHelpBinding binding = ActivityHelpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.toolbar.setTitle(R.string.help_title);

        binding.helpContent.setText(Html.fromHtml(getString(R.string.help_body), Html.FROM_HTML_MODE_COMPACT));
        binding.helpContent.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
