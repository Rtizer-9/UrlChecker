package com.trianguloy.urlchecker.modules;

import android.view.View;
import android.widget.TextView;

import com.trianguloy.urlchecker.R;

public class DescriptionConfig extends AModuleConfig {

    private final String description;

    public DescriptionConfig(String description) {
        this.description = description;
    }

    @Override
    public boolean canBeEnabled() {
        return true;
    }

    @Override
    public int getLayoutId() {
        return R.layout.config_description;
    }

    @Override
    public void onInitialize(View views) {
        ((TextView) views).setText(description);
    }
}