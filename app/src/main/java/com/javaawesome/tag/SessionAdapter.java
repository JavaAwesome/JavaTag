package com.javaawesome.tag;

import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.amazonaws.amplify.generated.graphql.ListSessionsQuery;

import java.util.List;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.SessionViewHolder> {

    public OnSessionInteractionListener listener;
    List<ListSessionsQuery.Item> sessions;

    public SessionAdapter(List<ListSessionsQuery.Item> sessions, OnSessionInteractionListener listener) {
        this.sessions = sessions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SessionAdapter.SessionViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
        final View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyler_view_sessions, parent, false);
        final SessionViewHolder holder = new SessionViewHolder(v);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(parent.getContext())
                    .setTitle("Join game?")
                    .setMessage("Would you like to join this game?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
//                            listener.addPlayerToChosenGame(holder.session);
                            listener.joinExistingGameSession(holder.session);
                            //TODO:  Save the users ID to the database based on the session that they clicked
                        }
                    })
                    .setNegativeButton("No", null).show();
            }
        });
        return holder;
    };

    public static class SessionViewHolder extends RecyclerView.ViewHolder {

        ListSessionsQuery.Item session;
        TextView sessionTitle;
//        TextView numberOfPlayers;

        public SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            this.sessionTitle = itemView.findViewById(R.id.session_title);
//            this.numberOfPlayers = itemView.findViewById(R.id.session_total_players);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull SessionAdapter.SessionViewHolder holder, int position) {
        ListSessionsQuery.Item sessionAtPosition = this.sessions.get(position);
        holder.session = sessionAtPosition;
        holder.sessionTitle.setText(sessionAtPosition.title());
//        holder.numberOfPlayers.setText("Population: " + sessionAtPosition.players());
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    public static interface OnSessionInteractionListener {
        public void joinExistingGameSession(ListSessionsQuery.Item session);
//        public void addPlayerToChosenGame(ListSessionsQuery.Item session);
    }
}
