package pt.isel.pdm.grupo17.thothnews.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.Html;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import pt.isel.pdm.grupo17.thothnews.R;
import pt.isel.pdm.grupo17.thothnews.activities.ClassesActivity;
import pt.isel.pdm.grupo17.thothnews.data.ThothContract;
import pt.isel.pdm.grupo17.thothnews.utils.ParseUtils;
import pt.isel.pdm.grupo17.thothnews.utils.UriUtils;

import static pt.isel.pdm.grupo17.thothnews.utils.ParseUtils.d;
import static pt.isel.pdm.grupo17.thothnews.utils.ParseUtils.readAllFrom;
import static pt.isel.pdm.grupo17.thothnews.utils.TagUtils.TAG_ACTIVITY;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 */
public class ThothUpdateService extends IntentService {

    private static final String SERVICE_TAG = "ThothUpdateService";
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_NEWS_UPDATE = "pt.isel.pdm.grupo17.thothnews.services.action.NEWS_UPDATE";
    private static final String ACTION_CLASS_NEWS_UPDATE = "pt.isel.pdm.grupo17.thothnews.services.action.CLASS_NEWS_UPDATE";
    private static final String ACTION_CLASS_PARTICIPANTS_UPDATE = "pt.isel.pdm.grupo17.thothnews.services.action.CLASS_PARTICIPANTS_UPDATE";
    private static final String ACTION_CLASSES_UPDATE = "pt.isel.pdm.grupo17.thothnews.services.action.CLASSES_UPDATE";

    private static final String ARG_CLASS_ID = "pt.isel.pdm.grupo17.thothnews.services.extra.ARG_CLASS_ID";
    private static final int ARG_CLASS_ID_DEFAULT_VALUE = -1;

    public static final String URI_API_ROOT = "http://thoth.cc.e.ipl.pt/api/v1";
    public static final String URI_CLASSES_LIST = URI_API_ROOT + "/classes";
    public static final String URI_CLASS_NEWS = URI_API_ROOT + "/classes/%d/newsitems";
    public static final String URI_NEWS_INFO = URI_API_ROOT + "/newsitems/%d";
    public static final String URI_CLASS_PARTICIPANTS = URI_API_ROOT + "/classes/%d/participants";
    public static final String URI_STUDENTS_INFO = URI_API_ROOT + "/students/%d";

    private static final int COLUMN_CLASS_ID = 0;

    class JsonThothClass{
        public static final String ID = "id";
        public static final String FULLNAME = "fullName";
        public static final String COURSE_NAME = "courseUnitShortName";
        public static final String LECTIVE_SEMESTER = "lectiveSemesterShortName";
        public static final String NAME = "className";
        public static final String TEACHER = "mainTeacherShortName";
    }
    class JsonThothNew {
        /** Array **/
        public static final String ARRAY_NEWS_ITEMS = "newsItems";
        /** Props **/
        public static final String ID = "id";
        public static final String TITLE = "title";
        public static final String WHEN = "when";
        public static final String CONTENT = "content";
    }
    class JsonThothParticipant{
        /** Array **/
        public static final String ARRAY_STUDENTS = "students";
        /** Props **/
        public static final String ID = "id";
//        public static final String NUMBER = "number";
        public static final String FULL_NAME = "fullName";
        public static final String ACADEMIC_EMAIL = "academicEmail";
//        public static final String AVATAR_URL = "avatarUrl"; // TODO: OBJECT -> props size24, size32, size64, size128
        public static final String ENROLL_DATE = "enrollmentDate";
        public static final String GROUP = "currentGroup";
    }


    public ThothUpdateService() {
        super("ThothUpdateService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            switch (intent.getAction()){
                case ACTION_NEWS_UPDATE:
                    handleNewsUpdate();
                    break;
                case ACTION_CLASSES_UPDATE:
                    handleClassesUpdate();
                    break;
                case ACTION_CLASS_NEWS_UPDATE:
                    long classID = intent.getLongExtra(ARG_CLASS_ID, ARG_CLASS_ID_DEFAULT_VALUE);
                    handleClassNewsUpdate(classID);
                    break;
                case ACTION_CLASS_PARTICIPANTS_UPDATE:
                    classID = intent.getLongExtra(ARG_CLASS_ID, ARG_CLASS_ID_DEFAULT_VALUE);
                    handleClassParticipantsUpdate(classID);
            }
        }
    }

    /**
     * Starts this service to perform action NewsUpdate, updating the news list of all classes enrolled.
     * If the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionNewsUpdate(Context context) {
        Intent intent = new Intent(context, ThothUpdateService.class);
        intent.setAction(ACTION_NEWS_UPDATE);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action ClassesUpdate, updating classes list, showing all available.
     * If the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionClassesUpdate(Context context) {
        Intent intent = new Intent(context, ThothUpdateService.class);
        intent.setAction(ACTION_CLASSES_UPDATE);
        context.startService(intent);
    }
    /**
     * Starts this service to perform action ClassNewsUpdate, with the given classID.
     * This will update the news list for a given class.
     * If the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionClassNewsUpdate(Context context, long classID) {
        if(classID == ARG_CLASS_ID_DEFAULT_VALUE) return;

        Intent intent = new Intent(context, ThothUpdateService.class);
        intent.setAction(ACTION_CLASS_NEWS_UPDATE);
        intent.putExtra(ARG_CLASS_ID, classID);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action ClassNewsUpdate, with the given classID.
     * This will update the news list for a given class.
     * If the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionClassParticipantsUpdate(Context context, long classID) {
        if(classID == ARG_CLASS_ID_DEFAULT_VALUE) return;

        Intent intent = new Intent(context, ThothUpdateService.class);
        intent.setAction(ACTION_CLASS_PARTICIPANTS_UPDATE);
        intent.putExtra(ARG_CLASS_ID, classID);
        context.startService(intent);
    }

    /**
     * Handle action ClassesUpdate in the provided background thread with the provided
     * parameters.
     */
    private void handleClassesUpdate() {
        try{
            URL url = new URL(URI_CLASSES_LIST);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            try {
                InputStream is = c.getInputStream();
                String data = readAllFrom(is);
                final JSONArray thothClasses = ParseUtils.parseClasses(data);
                ContentResolver resolver = getContentResolver();
                // TODO: Insert in batch instead of one by one
                for(int idx = 0; idx < thothClasses.length();++idx){
                    JSONObject jclass = thothClasses.getJSONObject(idx);
                    ContentValues currValues = new ContentValues();
                    currValues.put(ThothContract.Clazz._ID,jclass.getInt(JsonThothClass.ID));
                    currValues.put(ThothContract.Clazz.FULL_NAME,jclass.getString(JsonThothClass.FULLNAME));
                    currValues.put(ThothContract.Clazz.COURSE,jclass.getString(JsonThothClass.COURSE_NAME));
                    currValues.put(ThothContract.Clazz.SEMESTER,jclass.getString(JsonThothClass.LECTIVE_SEMESTER));
                    currValues.put(ThothContract.Clazz.SHORT_NAME,jclass.getString(JsonThothClass.NAME));
                    currValues.put(ThothContract.Clazz.TEACHER,jclass.getString(JsonThothClass.TEACHER));
                    resolver.insert(ThothContract.Clazz.CONTENT_URI,currValues);
                }
            } catch (JSONException e) {
                Log.e(SERVICE_TAG, "ERROR: handleClassesUpdate(..) while parsing JSON response");
                Log.e(SERVICE_TAG, e.getMessage());
            } finally {
                c.disconnect();
            }
        }catch (MalformedURLException e) {
            d(TAG_ACTIVITY,"An error ocurred while trying to create URL to request classes list!\nMessage: "+e.getMessage());
        } catch (IOException e) {
            d(TAG_ACTIVITY,"An error ocurred while trying to estabilish connection to Thoth API!\nMessage: "+e.getMessage());
        }
    }

    /**
     * Handle action NewsUpdate in the provided background.
     */
    private void handleNewsUpdate() {
        ContentResolver resolver = getContentResolver();
        Cursor cursor = resolver.query(ThothContract.Clazz.ENROLLED_URI, new String[]{ThothContract.Clazz._ID}
                ,String.format("%s = 1",ThothContract.Clazz.ENROLLED), null, null);

        long classID;
        while(cursor.moveToNext()){
            classID = cursor.getLong(COLUMN_CLASS_ID);
            handleClassNewsUpdate(classID);
        }
        cursor.close();
    }

    private final long[] mVibratePattern = { 0, 200, 200, 300 };

    /**
     * Handle action ClassNewsUpdate in the provided background thread with the provided
     * parameters.
     * @param classID
     */
    private void handleClassNewsUpdate(long classID) {
        if( classID == ARG_CLASS_ID_DEFAULT_VALUE){
            return;
        }
        int addedNews = 0;
        try {
            String urlPath = String.format(URI_CLASS_NEWS,classID);
            URL url = new URL(urlPath);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            try {
                InputStream is = c.getInputStream();
                String data = readAllFrom(is);
                is.close();
                final JSONArray thothNews = ParseUtils.parseElement(data, JsonThothNew.ARRAY_NEWS_ITEMS);
                ContentResolver resolver = getContentResolver();
                // TODO: Insert in batch instead of one by one
                // TODO: Only make request for content of the news that don't exist in the database
                Uri classNewsUri = UriUtils.Classes.parseNewsFromClassID(classID);
                Cursor classNewsIDsCursor = resolver.query(classNewsUri, new String[]{ThothContract.News._ID},null,null,null);
                List<Long> classNewsIDs = getListFromCursor(classNewsIDsCursor);
                classNewsIDsCursor.close();
                long currNewID;
                for (int idx = 0; idx < thothNews.length(); ++idx) {
                    JSONObject jnews = thothNews.getJSONObject(idx), jnewsDetails;
                    currNewID = jnews.getLong(JsonThothNew.ID);
                    if(!classNewsIDs.contains(currNewID)){

                        jnewsDetails = getNewDetails(currNewID);
                        ContentValues currValues = new ContentValues();
                        currValues.put(ThothContract.News._ID,jnews.getInt(JsonThothNew.ID));
                        currValues.put(ThothContract.News.TITLE,jnews.getString(JsonThothNew.TITLE));

                        String when = jnews.getString(JsonThothNew.WHEN);
                        currValues.put(ThothContract.News.WHEN_CREATED, when);
                        String content = String.valueOf(Html.fromHtml(jnewsDetails.getString(JsonThothNew.CONTENT)));
                        currValues.put(ThothContract.News.CONTENT,content);
                        currValues.put(ThothContract.News.CLASS_ID,classID);
                        resolver.insert(ThothContract.News.CONTENT_URI, currValues);
                        ++addedNews;
                    }
                }
                if(addedNews >0){
                    Notification.Builder builder = new Notification.Builder(getApplicationContext())
                            .setContentTitle("You have news to read on!")
                            .setContentText("Click to open the application.")
                            .setAutoCancel(true)
                            .setSmallIcon(R.drawable.ic_thoth)
                            .setVibrate(mVibratePattern)
                            .setOngoing(false); //false => can drop notification on notification area, true => only dissapier if exectuted action for notification

                    Intent i = new Intent(getApplicationContext(), ClassesActivity.class);
                    PendingIntent pIntent = PendingIntent.getActivity(getApplicationContext(), 1, i, 0);
                    builder.setContentIntent(pIntent);

                    NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.notify(0, builder.build());
                }
            } catch (JSONException e) {
                Log.e(SERVICE_TAG, "ERROR: handleClassNewsUpdate(..) while parsing JSON response");
                Log.e(SERVICE_TAG, e.getMessage());
            } finally {
                c.disconnect();
            }
        } catch (MalformedURLException e) {
            d(TAG_ACTIVITY, "An error occurred while trying to create URL to request news list given classID:" + classID + "\nMessage: " + e.getMessage());
        } catch (IOException e) {
            d(TAG_ACTIVITY, "An error ocurred while trying to estabilish connection to Thoth API!\nMessage: " + e.getMessage());
        }
    }

    /**
     * Handle action ClassParticipantsUpdate in the provided background thread with the provided
     * parameters.
     * @param classID
     */
    private void handleClassParticipantsUpdate(long classID) {
        if( classID == ARG_CLASS_ID_DEFAULT_VALUE){
            return;
        }
        try {
            String urlPath = String.format(URI_CLASS_PARTICIPANTS,classID);
            URL url = new URL(urlPath);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            try {
                InputStream is = c.getInputStream();
                String data = readAllFrom(is);
                is.close();
                final JSONArray thothParticipants = ParseUtils.parseElement(data, JsonThothParticipant.ARRAY_STUDENTS);
                ContentResolver resolver = getContentResolver();
                // TODO: Insert in batch instead of one by one
                // TODO: Only make request for content of the news that don't exist in the database
                Uri classNewsUri = UriUtils.Classes.parseParticipantsFromClassID(classID);
                Cursor classParticipantsIDsCursor = resolver.query(classNewsUri, new String[]{ThothContract.Students._ID},null,null,null);
                List<Long>classParticipantsIDs = getListFromCursor(classParticipantsIDsCursor);
                classParticipantsIDsCursor.close();
                long currParticipantID;
                for (int idx = 0; idx < thothParticipants.length(); ++idx) {
                    JSONObject jnews = thothParticipants.getJSONObject(idx);
                    currParticipantID = jnews.getLong(JsonThothParticipant.ID);
                    if(!classParticipantsIDs.contains(currParticipantID)){
                        ContentValues currValues = new ContentValues();
                        currValues.put(ThothContract.Students._ID,jnews.getInt(JsonThothParticipant.ID));
//                        currValues.put(ThothContract.Students.NUMBER,jnews.getInt(JsonThothParticipant.NUMBER)); ID == NUMBER
                        currValues.put(ThothContract.Students.FULL_NAME,jnews.getString(JsonThothParticipant.FULL_NAME));
                        currValues.put(ThothContract.Students.ACADEMIC_EMAIL,jnews.getString(JsonThothParticipant.ACADEMIC_EMAIL));
//                        currValues.put(ThothContract.Participants.AVATAR_URL,jnews.getString(JsonThothParticipant.AVATAR_URL)); /* TODO */
                        String group = jnews.getString(JsonThothParticipant.GROUP);
                        int nGroup = (group.equals("-") ? 0 : Integer.valueOf(group));
                        currValues.put(ThothContract.Students.GROUP, nGroup);
                        currValues.put(ThothContract.Students.ENROLLED_DATE, jnews.getString(JsonThothParticipant.ENROLL_DATE));
                        currValues.put(ThothContract.Students.CLASS_ID,classID);
                        resolver.insert(ThothContract.Students.CONTENT_URI, currValues); /* TODO review */
                    }
                }
            } catch (JSONException e) {
                Log.e(SERVICE_TAG, "ERROR: handleClassNewsUpdate(..) while parsing JSON response");
                Log.e(SERVICE_TAG, e.getMessage());
            } finally {
                c.disconnect();
            }
        } catch (MalformedURLException e) {
            d(TAG_ACTIVITY, "An error occurred while trying to create URL to request participants list given classID:" + classID + "\nMessage: " + e.getMessage());
        } catch (IOException e) {
            d(TAG_ACTIVITY, "An error ocurred while trying to estabilish connection to Thoth API!\nMessage: " + e.getMessage());
        }
    }

    /**
     * Receives a cursor with only one column of type long and returns a List of the column values
     * @param c Cursor with only one column composed of IDs of type long
     * @return List<Long>
     */
    private static List<Long> getListFromCursor(Cursor c){
        List<Long> ids = new ArrayList<>();
        while (c.moveToNext()){
            ids.add(c.getLong(0));
        }
        return ids;
    }

    private static JSONObject getNewDetails(long newID) throws IOException, JSONException {
        URL newsURL = new URL(String.format(URI_NEWS_INFO, newID));
        HttpURLConnection newsConn = (HttpURLConnection) newsURL.openConnection();
        InputStream newsStream = newsConn.getInputStream();
        String newsData = readAllFrom(newsStream);
        newsStream.close();
        return new JSONObject(newsData);
    }

    private static JSONObject getParicipantDetails(long studentID) throws IOException, JSONException {
        URL studentsURL = new URL(String.format(URI_STUDENTS_INFO, studentID));
        HttpURLConnection studentsConn = (HttpURLConnection) studentsURL.openConnection();
        InputStream studentsStream = studentsConn.getInputStream();
        String studentsData = readAllFrom(studentsStream);
        studentsStream.close();
        return new JSONObject(studentsData);
    }

}
