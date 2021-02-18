package org.helllabs.android.xmp.modarchive.result;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.XmpApplication;
import org.helllabs.android.xmp.modarchive.Downloader;
import org.helllabs.android.xmp.modarchive.Search;
import org.helllabs.android.xmp.modarchive.model.Module;
import org.helllabs.android.xmp.modarchive.model.Sponsor;
import org.helllabs.android.xmp.modarchive.request.ModuleRequest;
import org.helllabs.android.xmp.modarchive.response.HardErrorResponse;
import org.helllabs.android.xmp.modarchive.response.ModArchiveResponse;
import org.helllabs.android.xmp.modarchive.response.ModuleResponse;
import org.helllabs.android.xmp.modarchive.response.SoftErrorResponse;
import org.helllabs.android.xmp.player.PlayerActivity;
import org.helllabs.android.xmp.preferences.Preferences;
import org.helllabs.android.xmp.util.Log;
import org.helllabs.android.xmp.util.Message;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ModuleResult extends Result implements ModuleRequest.OnResponseListener, Downloader.DownloaderListener {
    private static final String TAG = "ModuleResult";
    private static final String MODARCHIVE_DIRNAME = "TheModArchive";
    private TextView title;
    private TextView filename;
    private TextView info;
    private TextView instruments;
    private TextView license;
    private TextView licenseDescription;
    private TextView sponsorText;
    private Module module;
    private Downloader downloader;
    private Button downloadButton;
    private Button deleteButton;
    private Button playButton;
    private TextView errorMessage;
    private View dataView;

    private SharedPreferences mPrefs;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.result_module);
        setupCrossfade();

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        title = (TextView) findViewById(R.id.module_title);
        filename = (TextView) findViewById(R.id.module_filename);
        info = (TextView) findViewById(R.id.module_info);
        instruments = (TextView) findViewById(R.id.module_instruments);
        license = (TextView) findViewById(R.id.module_license);
        licenseDescription = (TextView) findViewById(R.id.module_license_description);
        sponsorText = (TextView) findViewById(R.id.module_sponsor);

        downloadButton = (Button) findViewById(R.id.module_download);
        downloadButton.setEnabled(false);

        deleteButton = (Button) findViewById(R.id.module_delete);
        deleteButton.setEnabled(false);

        playButton = (Button) findViewById(R.id.module_play);
        playButton.setEnabled(false);

        errorMessage = (TextView) findViewById(R.id.error_message);
        dataView = findViewById(R.id.result_data);

        downloader = new Downloader(this);
        downloader.setDownloaderListener(this);

        final long id = getIntent().getLongExtra(Search.MODULE_ID, -1);
        Log.d(TAG, "request module ID " + id);
        makeRequest(String.valueOf(id));
    }

    protected void makeRequest(final String query) {
        final String key = getString(R.string.modarchive_apikey);
        try {
            final ModuleRequest request = new ModuleRequest(key, ModuleRequest.MODULE, query);
            request.setOnResponseListener(this).send();
        } catch (UnsupportedEncodingException e) {
            handleQueryError();
        }
    }


    // ModuleRequest callbacks

    @Override
    public void onResponse(final ModArchiveResponse response) {

        final ModuleResponse moduleList = (ModuleResponse) response;
        if (moduleList.isEmpty()) {
            dataView.setVisibility(View.GONE);
        } else {
            final Module module = moduleList.get(0);
            Log.i(TAG, "Response: title=" + module.getSongTitle());
            title.setText(module.getSongTitle());
            filename.setText(module.getFilename());
            final int size = module.getBytes() / 1024;
            info.setText(String.format("%s by %s (%d KB)", module.getFormat(), module.getArtist(), size));
            license.setText(Html.fromHtml("License: <a href=\"" + module.getLegalUrl() + "\">" + module.getLicense() + "</a>"));
            license.setMovementMethod(LinkMovementMethod.getInstance());
            licenseDescription.setText(module.getLicenseDescription());
            instruments.setText(module.getInstruments());
            this.module = module;

            updateButtons(module);
        }

        final Sponsor sponsor = response.getSponsor();
        if (sponsor != null) {
            sponsorText.setText(Html.fromHtml("Download mirrors provided by <a href=\"" + sponsor.getLink() + "\">" + sponsor.getName() + "</a>"));
            sponsorText.setMovementMethod(LinkMovementMethod.getInstance());
        }

        crossfade();
    }

    @Override
    public void onSoftError(final SoftErrorResponse response) {
        errorMessage.setText(response.getMessage());
        dataView.setVisibility(View.GONE);
        crossfade();
    }

    @Override
    public void onHardError(final HardErrorResponse response) {
        handleError(response.getError());
    }

    // DownloaderListener callbacks

    @Override
    public void onSuccess() {
        updateButtons(module);
    }

    @Override
    public void onFailure() {
        // do nothing
    }

    // Button click handlers

    public void downloadClick(final View view) {
        final String modDir = getDownloadPath(module);
        final String url = module.getUrl();

        Log.i(TAG, "Download " + url + " to " + modDir);
        downloader.download(url, modDir, module.getBytes());
    }

    public void deleteClick(final View view) {
        final File file = localFile(module);

        Message.yesNoDialog(this, "Delete file", "Are you sure you want to delete " + module.getFilename() + "?", () -> {
            Log.i(TAG, "Delete " + file.getPath());
            if (file.delete()) {
                updateButtons(module);
            } else {
                Message.toast(ModuleResult.this, "Error");
            }

            // Delete parent directory if empty
            if (mPrefs.getBoolean(Preferences.ARTIST_FOLDER, true)) {
                final File parent = file.getParentFile();
                final File[] contents = parent.listFiles();
                if (contents != null && contents.length == 0) {
                    try {
                        final String mediaPath = new File(mPrefs.getString(Preferences.MEDIA_PATH, Preferences.DEFAULT_MEDIA_PATH)).getCanonicalPath();
                        final String parentPath = parent.getCanonicalPath();

                        if (parentPath.startsWith(mediaPath) && !parentPath.equals(mediaPath)) {
                            Log.i(TAG, "Remove empty directory " + parent.getPath());
                            if (!parent.delete()) {
                                Message.toast(ModuleResult.this, "Error removing directory");
                                Log.e(TAG, "error removing directory");
                            }
                        }
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            }
        });
    }

    public void playClick(final View view) {
        final String path = localFile(module).getPath();
        final List<String> modList = new ArrayList<>();

        modList.add(path);

        final Intent intent = new Intent(this, PlayerActivity.class);
        ((XmpApplication) getApplication()).setFileList(modList);
        intent.putExtra(PlayerActivity.PARM_START, 0);
        Log.i(TAG, "Play " + path);
        startActivity(intent);
    }

    private String getDownloadPath(final Module module) {
        final StringBuilder sb = new StringBuilder();

        sb.append(mPrefs.getString(Preferences.MEDIA_PATH, Preferences.DEFAULT_MEDIA_PATH));

        if (mPrefs.getBoolean(Preferences.MODARCHIVE_FOLDER, true)) {
            sb.append(File.separatorChar);
            sb.append(MODARCHIVE_DIRNAME);
        }

        if (mPrefs.getBoolean(Preferences.ARTIST_FOLDER, true)) {
            sb.append(File.separatorChar);
            sb.append(module.getArtist());
        }

        return sb.toString();
    }

    private void updateButtons(final Module module) {
        final boolean exists = localFile(module).exists();
        downloadButton.setEnabled(true);
        deleteButton.setEnabled(exists);
        playButton.setEnabled(exists);
    }

    private File localFile(final Module module) {
        final String path = getDownloadPath(module);
        final String url = module.getUrl();
        final String filename = url.substring(url.lastIndexOf('#') + 1);
        return new File(path, filename);
    }
}
