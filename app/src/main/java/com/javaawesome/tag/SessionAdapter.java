package com.javaawesome.tag;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
    public SessionAdapter.SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_recycler_view_sessions, parent, false);
        final SessionViewHolder holder = new SessionViewHolder(v);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.joinExistingGameSession(holder.session);
            }
        });
        return holder;
    }

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
        holder.sessionTitle.setText("Title: " + sessionAtPosition.title());
//        holder.numberOfPlayers.setText("Players: " + sessionAtPosition);
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    public static interface OnSessionInteractionListener {
        public void joinExistingGameSession(ListSessionsQuery.Item session);
    }
}
