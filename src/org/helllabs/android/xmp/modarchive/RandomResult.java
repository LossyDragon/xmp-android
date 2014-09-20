package org.helllabs.android.xmp.modarchive;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.modarchive.request.ModuleRequest;


public class RandomResult extends ModuleResult {
	
	@Override
	protected void makeRequest(final long id) {
		final String key = getString(R.string.modarchive_apikey);
		final ModuleRequest request = new ModuleRequest(key, "random");
		request.setOnResponseListener(this).send();
	}
}
