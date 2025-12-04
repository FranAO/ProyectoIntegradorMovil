package com.example.proyectointegrador.UI;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectointegrador.Adapters.TicketAdapter;
import com.example.proyectointegrador.Models.Ticket;
import com.example.proyectointegrador.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class HistoryActivity extends BaseNavigationActivity {

    private RecyclerView recyclerTickets;
    private TicketAdapter ticketAdapter;
    private ArrayList<Ticket> ticketList;
    private CardView emptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);

        setupNavigation();
        initViews();
        loadTickets();
    }

    private void initViews() {
        recyclerTickets = findViewById(R.id.recyclerTickets);
        emptyState = findViewById(R.id.emptyState);

        recyclerTickets.setLayoutManager(new LinearLayoutManager(this));
        ticketList = new ArrayList<>();
        ticketAdapter = new TicketAdapter(this, ticketList);
        recyclerTickets.setAdapter(ticketAdapter);
    }

    private void loadTickets() {
        SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
        String userEmail = prefs.getString("LOGGED_IN_USER_EMAIL", "");

        if (!userEmail.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerTickets.setVisibility(View.GONE);
            
            com.example.proyectointegrador.Services.TicketApiService.getAvailableTickets(
                    userEmail,
                    new com.example.proyectointegrador.Services.TicketApiService.ApiCallback() {
                        @Override
                        public void onSuccess(String response) {
                            try {
                                ticketList.clear();
                                JSONArray ticketsArray = new JSONArray(response);
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                                
                                for (int i = 0; i < ticketsArray.length(); i++) {
                                    JSONObject ticketJson = ticketsArray.getJSONObject(i);
                                    
                                    Date purchaseDate = null;
                                    String dateStr = ticketJson.optString("purchaseDate");
                                    if (dateStr != null && !dateStr.isEmpty()) {
                                        try {
                                            purchaseDate = sdf.parse(dateStr);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    
                                    Ticket ticket = new Ticket(
                                            ticketJson.getString("id"),
                                            ticketJson.getString("studentId"),
                                            ticketJson.optString("packageId"),
                                            ticketJson.optString("tripId"),
                                            ticketJson.getString("status"),
                                            purchaseDate
                                    );
                                    // Solo mostrar tickets INDIVIDUALES disponibles
                                    // NO mostrar: usados, cancelados, ni tickets de paquetes
                                    String status = ticket.getStatus();
                                    String packageId = ticket.getPackageId();
                                    boolean isFromPackage = packageId != null && !packageId.isEmpty() && !"null".equals(packageId);
                                    
                                    if ("available".equals(status) && !isFromPackage) {
                                        ticketList.add(ticket);
                                    }
                                }
                                
                                ticketAdapter.notifyDataSetChanged();
                                
                                if (ticketList.isEmpty()) {
                                    emptyState.setVisibility(View.VISIBLE);
                                    recyclerTickets.setVisibility(View.GONE);
                                } else {
                                    emptyState.setVisibility(View.GONE);
                                    recyclerTickets.setVisibility(View.VISIBLE);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                Toast.makeText(HistoryActivity.this, "Error al cargar tickets", Toast.LENGTH_SHORT).show();
                                emptyState.setVisibility(View.VISIBLE);
                                recyclerTickets.setVisibility(View.GONE);
                            }
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(HistoryActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                            emptyState.setVisibility(View.VISIBLE);
                            recyclerTickets.setVisibility(View.GONE);
                        }
                    }
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTickets();
    }

    @Override
    protected int getNavigationIndex() {
        return 3;
    }
}