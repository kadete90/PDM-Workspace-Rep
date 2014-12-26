package pt.isel.pdm.grupo17.thothnews.fragments;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;

import pt.isel.pdm.grupo17.thothnews.R;
import pt.isel.pdm.grupo17.thothnews.activities.ClassSectionsActivity;
import pt.isel.pdm.grupo17.thothnews.adapters.ClassesPickAdapter;
import pt.isel.pdm.grupo17.thothnews.data.ThothContract;
import pt.isel.pdm.grupo17.thothnews.models.ThothClass;
import pt.isel.pdm.grupo17.thothnews.services.ThothUpdateService;
import pt.isel.pdm.grupo17.thothnews.utils.ConnectionUtils;
import pt.isel.pdm.grupo17.thothnews.utils.SQLiteUtils;
import pt.isel.pdm.grupo17.thothnews.utils.TagUtils;
import pt.isel.pdm.grupo17.thothnews.utils.UriUtils;
import pt.isel.pdm.grupo17.thothnews.view.MultiSwipeRefreshLayout;

public class ClassesPickFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, SearchView.OnQueryTextListener{

    static final int CLASSES_SELECTION_CURSOR_LOADER_ID = 1;
    static final String[] CURSOR_COLUMNS = {ThothContract.Classes._ID, ThothContract.Classes.FULL_NAME, ThothContract.Classes.TEACHER_NAME, ThothContract.Classes.SHORT_NAME,
                                            ThothContract.Classes.SEMESTER, ThothContract.Classes.COURSE, ThothContract.Classes.TEACHER_ID, ThothContract.Classes.ENROLLED};
    static final String ORDER_BY = ThothContract.Classes.SEMESTER + " DESC" + ", " + ThothContract.Classes.COURSE;

    private MultiSwipeRefreshLayout mSwipeRefreshLayout;
    private GridView mGridView;
    private View mEmptyView;
    private ClassesPickAdapter mListAdapter;
    private String mCurFilter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_grid_classes_pick, container, false);

        mSwipeRefreshLayout = (MultiSwipeRefreshLayout) view.findViewById(R.id.swiperefresh);
        mGridView = (GridView) view.findViewById(android.R.id.list);
        mEmptyView = view.findViewById(android.R.id.empty);

        final FragmentActivity activity = getActivity();

        final Button cancelBtn = (Button) view.findViewById(R.id.BtnDiscard);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateClassesSelection(activity, false);
            }
        });

        final Button okBtn = (Button) view.findViewById(R.id.BtnSave);
        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateClassesSelection(activity, true);
            }
        });

        return view;
    }

    public void updateClassesSelection(FragmentActivity activity, boolean toSave){
        if(mListAdapter.getMapSelection().isEmpty()) {
            activity.finish();
            return;
        }
        for(Map.Entry<Long, ClassesPickAdapter.SelectionState> entryClass : mListAdapter.getMapSelection().entrySet()) {
            ContentValues values = new ContentValues();
            boolean enrolled = ((toSave) ? entryClass.getValue().finalState : entryClass.getValue().initialState);
            values.put(ThothContract.Classes.ENROLLED, enrolled ? SQLiteUtils.TRUE : SQLiteUtils.FALSE);
            activity.getContentResolver().update(UriUtils.Classes.parseClass(entryClass.getKey()), values, null, null );
            if(toSave && enrolled)
                ThothUpdateService.startActionClassNewsUpdate(activity, entryClass.getKey());
        }
        Toast.makeText(activity.getApplicationContext(),getString((toSave)? R.string.classes_pick_ok : R.string.classes_pick_cancel), Toast.LENGTH_LONG).show();
        activity.finish();
    }

    /**runs after the View (onCreateView) has been created.**/
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mListAdapter = new ClassesPickAdapter(getActivity());

        mGridView.setAdapter(mListAdapter);
        mGridView.setEmptyView(mEmptyView);
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ThothClass thothClass = (ThothClass) mListAdapter.getItem(position);
                Intent i = new Intent(getActivity(), ClassSectionsActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.putExtra(TagUtils.TAG_SERIALIZABLE_CLASS, thothClass);
                startActivity(i);
            }
        });

        mSwipeRefreshLayout.setSwipeableChildren(android.R.id.list, android.R.id.empty);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshAndUpdate();
            }
        });

        Cursor classesCursor = getActivity().getContentResolver()
                .query(ThothContract.Classes.CONTENT_URI, null, null, null, null);

        if(classesCursor.moveToNext()){
            classesCursor.close();
            getLoaderManager().initLoader(CLASSES_SELECTION_CURSOR_LOADER_ID, null, this);
        }
        else {
            classesCursor.close();
            Toast.makeText(getActivity(),getString(R.string.toast_wait_message),Toast.LENGTH_LONG).show();
            refreshAndUpdate();
        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri baseUri;
        if (mCurFilter != null) {
            baseUri = Uri.withAppendedPath(ThothContract.Classes.SEARCH_URI,
                    Uri.encode(mCurFilter));
        } else {
            baseUri = ThothContract.Classes.CONTENT_URI;
        }
        return new CursorLoader(getActivity(), baseUri, CURSOR_COLUMNS, null, null, ORDER_BY);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mListAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mListAdapter.swapCursor(null);
    }

    public void refreshAndUpdate() {
        if(!ConnectionUtils.isConnected(getActivity()))
            return;

        mSwipeRefreshLayout.setRefreshing(true);
        ThothUpdateService.startActionClassesUpdate(getActivity());
        getLoaderManager().restartLoader(CLASSES_SELECTION_CURSOR_LOADER_ID, null, this);
        mSwipeRefreshLayout.setRefreshing(false);
    }

   public void myCreateOptionsMenu(Menu menu) {
        MenuItem item = menu.add("Search");
        item.setIcon(android.R.drawable.ic_menu_search);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        SearchView sv = new SearchView(getActivity());
        sv.setOnQueryTextListener(this);
        int id = sv.getContext().getResources().getIdentifier("android:id/search_src_text", null, null);
        ((TextView) sv.findViewById(id)).setTextColor(Color.WHITE);
        item.setActionView(sv);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        mCurFilter = !TextUtils.isEmpty(newText) ? newText : null;
        getLoaderManager().restartLoader(CLASSES_SELECTION_CURSOR_LOADER_ID, null, this);
        return true;
    }
}