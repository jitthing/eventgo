package com.event_go.notification_service.config;

import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class twilioConfig {

    @Value("${twilio.account_sid}")
    private String accountSid;


    @Value("${twilio.auth_token}")
    private String authToken;

//    @Value("${twilio.phone_number}")
//    private String phoneNumber;

    @PostConstruct
    public void twilioInit() {
        Twilio.init(accountSid, authToken);
        System.out.println("Twilio initialised with Account SID: " + accountSid);
    }


}
