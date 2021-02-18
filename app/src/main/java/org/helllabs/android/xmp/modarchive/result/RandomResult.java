package org.helllabs.android.xmp.modarchive.result;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.modarchive.request.ModuleRequest;

import android.os.Bundle;

public class RandomResult extends ModuleResult implements ModuleRequest.OnResponseListener {
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.search_random_title);
    }

    @Override
    protected void makeRequest(final String query) {
        final ModuleRequest request = new ModuleRequest(apiKey, ModuleRequest.RANDOM);
        request.setOnResponseListener(this).send();
    }
}
