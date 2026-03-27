package com.bvl.ticket.bean;

import com.bvl.ticket.model.DashboardStats;
import com.bvl.ticket.model.User;
import com.bvl.ticket.service.AuthService;
import com.bvl.ticket.service.TicketService;
import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.Getter;
import java.io.Serializable;

@Named
@ViewScoped
@Getter
public class AdminBean implements Serializable {

    @Inject
    private AuthService authService;
    
    @Inject
    private TicketService ticketService;
    
    private DashboardStats dashboardStats;
    
    @PostConstruct
    public void init() {
        // Dummy user for dashboard stats - in real app this would be current user
        User currentUser = new User();
        currentUser.setRole(com.bvl.ticket.model.UserRole.ADMIN);
        
        dashboardStats = ticketService.getDashboardStats(currentUser);
    }
}
