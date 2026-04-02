package com.example.eventsapp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class NotificationHelperTest {

    private FirebaseFirestore db;
    private CollectionReference usersCollection;
    private CollectionReference eventsCollection;
    private CollectionReference notificationsCollection;
    private CollectionReference logsCollection;

    private DocumentReference userDoc;
    private DocumentReference eventDoc;
    private DocumentReference notificationDoc;
    private DocumentReference logDoc;

    private DocumentSnapshot userSnapshot;
    private DocumentSnapshot eventSnapshot;

    private FirestoreNotificationHelper helper;

    @BeforeEach
    public void setUp() {
        db = Mockito.mock(FirebaseFirestore.class);

        usersCollection = Mockito.mock(CollectionReference.class);
        eventsCollection = Mockito.mock(CollectionReference.class);
        notificationsCollection = Mockito.mock(CollectionReference.class);
        logsCollection = Mockito.mock(CollectionReference.class);

        userDoc = Mockito.mock(DocumentReference.class);
        eventDoc = Mockito.mock(DocumentReference.class);
        notificationDoc = Mockito.mock(DocumentReference.class);
        logDoc = Mockito.mock(DocumentReference.class);

        userSnapshot = Mockito.mock(DocumentSnapshot.class);
        eventSnapshot = Mockito.mock(DocumentSnapshot.class);

        helper = new FirestoreNotificationHelper(db);

        when(db.collection("users")).thenReturn(usersCollection);
        when(db.collection("events")).thenReturn(eventsCollection);
        when(db.collection("notification_logs")).thenReturn(logsCollection);

        when(usersCollection.document("user123")).thenReturn(userDoc);
        when(eventsCollection.document("event123")).thenReturn(eventDoc);

        when(userDoc.collection("notifications")).thenReturn(notificationsCollection);
        when(notificationsCollection.document(anyString())).thenReturn(notificationDoc);
        when(logsCollection.document(anyString())).thenReturn(logDoc);

        when(eventSnapshot.getString("name")).thenReturn("Sample Event");
        when(eventSnapshot.getString("createdBy")).thenReturn("organizer123");
    }

    @Test
    public void waitlistedNotification_notWritten_whenNotificationsDisabled() {
        when(userSnapshot.getBoolean("notificationsEnabled")).thenReturn(false);
        when(userDoc.get()).thenReturn(Tasks.forResult(userSnapshot));

        helper.sendWaitlistedNotification("user123", "event123");

        verify(notificationDoc, never()).set(any());
    }

    @Test
    public void notSelectedNotification_notWritten_whenNotificationsDisabled() {
        when(userSnapshot.getBoolean("notificationsEnabled")).thenReturn(false);
        when(userDoc.get()).thenReturn(Tasks.forResult(userSnapshot));

        helper.sendNotSelectedNotification("user123", "event123");

        verify(notificationDoc, never()).set(any());
    }

    @Test
    public void invitationNotification_written_evenWhenNotificationsDisabled() {
        when(eventDoc.get()).thenReturn(Tasks.forResult(eventSnapshot));
        when(usersCollection.document("organizer123")).thenReturn(Mockito.mock(DocumentReference.class));

        helper.sendInvitationNotification("user123", "event123");

        verify(notificationDoc, times(1)).set(any());
    }
}