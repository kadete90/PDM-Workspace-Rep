package pt.isel.pdm.grupo17.thothnews.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import pt.isel.pdm.grupo17.thothnews.R;
import pt.isel.pdm.grupo17.thothnews.activities.ClassSectionsActivity;
import pt.isel.pdm.grupo17.thothnews.adapters.ParticipantsAdapter;
import pt.isel.pdm.grupo17.thothnews.data.ThothContract;
import pt.isel.pdm.grupo17.thothnews.models.ThothClass;
import pt.isel.pdm.grupo17.thothnews.services.ThothUpdateService;
import pt.isel.pdm.grupo17.thothnews.utils.UriUtils;
import pt.isel.pdm.grupo17.thothnews.view.MultiSwipeRefreshLayout;

public class ParticipantsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static ParticipantsFragment newInstance() {
        Bundle bundle = new Bundle();

        ParticipantsFragment fragment = new ParticipantsFragment();
        fragment.setArguments(bundle);

        return fragment;
    }

    static final int PARTICIPANTS_CURSOR_LOADER_ID = 3;
    static final String[] CURSOR_COLUMNS = {ThothContract.Students._ID, ThothContract.Students.FULL_NAME,
            ThothContract.Students.ACADEMIC_EMAIL, ThothContract.Students.ENROLLED_DATE, ThothContract.Students.GROUP};
            //ThothContract.Participants.AVATAR_URL

    private MultiSwipeRefreshLayout mSwipeRefreshLayout;
    private GridView mGridView;
    private View mEmptyView;
    private ParticipantsAdapter mListAdapter;

    private static ThothClass sThothClass;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_section_participants, container, false);
        mSwipeRefreshLayout = (MultiSwipeRefreshLayout) view.findViewById(R.id.swiperefresh);
        mGridView = (GridView) view.findViewById(android.R.id.list);
        mEmptyView = view.findViewById(android.R.id.empty);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sThothClass = ClassSectionsActivity.getThothClass();

        mListAdapter = new ParticipantsAdapter(getActivity());


        mGridView.setAdapter(mListAdapter);
        mGridView.setEmptyView(mEmptyView);

        mSwipeRefreshLayout.setSwipeableChildren(android.R.id.list, android.R.id.empty);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshLoader();
            }
        });

        getLoaderManager().initLoader(PARTICIPANTS_CURSOR_LOADER_ID, null, this);
    }

    @Override
    public void onResume(){
        super.onResume();
        if(mListAdapter.isEmpty())
            refreshLoader();
    }

    public boolean isFragmentUIActive() {
        return isVisible() && isAdded() && !isDetached() && !isRemoving();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        if(isFragmentUIActive()){
            String OrderBy = ThothContract.Students.GROUP + " DESC, " + ThothContract.Students._ID + " ASC";
            return new CursorLoader(getActivity(), UriUtils.Classes.parseParticipantsFromClassID(sThothClass.getID()),
                    CURSOR_COLUMNS, null, null, OrderBy);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mListAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mListAdapter.swapCursor(null);
    }

    public void refreshLoader() {
        if(!isFragmentUIActive())
            return;
        mSwipeRefreshLayout.setRefreshing(true);
        ThothUpdateService.startActionClassParticipantsUpdate(getActivity(), sThothClass.getID());
        getLoaderManager().restartLoader(PARTICIPANTS_CURSOR_LOADER_ID, null, this);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
           // TODO: request participants photos
        }
    }
}