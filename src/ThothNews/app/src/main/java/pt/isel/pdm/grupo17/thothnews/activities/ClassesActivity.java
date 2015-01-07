package pt.isel.pdm.grupo17.thothnews.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;

import pt.isel.pdm.grupo17.thothnews.R;
import pt.isel.pdm.grupo17.thothnews.fragments.ClassesFragment;
import pt.isel.pdm.grupo17.thothnews.utils.ConnectionUtils;
import pt.isel.pdm.grupo17.thothnews.utils.SQLiteUtils;
import pt.isel.pdm.grupo17.thothnews.utils.TagUtils;

public class ClassesActivity extends FragmentActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_frame_classes);

        SQLiteUtils.startPrefsIfNoClassesEnrolled(this);

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            ClassesFragment fragment = new ClassesFragment();
            transaction.replace(R.id.fragment_container_classes, fragment);
            transaction.commit();
        }

        getActionBar().setTitle(R.string.label_activity_classes);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_classes, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_webview:
                if(!ConnectionUtils.checkConnection(getApplicationContext(), true))
                    break;
                Intent intent = new Intent(this, WebViewActivity.class);
                intent.putExtra(TagUtils.TAG_EXTRA_WEB_VIEW_URL, WebViewActivity.URI_CLASSES_ROOT);
                startActivity(intent);
                return true;
            case R.id.action_settings:
                startActivity(new Intent(ClassesActivity.this, SettingsActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}