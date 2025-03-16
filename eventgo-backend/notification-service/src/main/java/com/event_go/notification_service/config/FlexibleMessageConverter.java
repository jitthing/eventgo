package com.event_go.notification_service.config;

import com.event_go.notification_service.model.NotificationEvent;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.lang.reflect.Field;

public class FlexibleMessageConverter implements MessageConverter {
    
    private final Jackson2JsonMessageConverter jsonConverter;
    private final SimpleMessageConverter javaConverter;
    
    public FlexibleMessageConverter() {
        this.jsonConverter = new Jackson2JsonMessageConverter();
        this.javaConverter = new SimpleMessageConverter();
    }
    
    @Override
    public Message toMessage(Object object, MessageProperties messageProperties) {
        // Use JSON for outgoing messages
        return jsonConverter.toMessage(object, messageProperties);
    }
    
    @Override
    public Object fromMessage(Message message) {
        try {
            MessageProperties props = message.getMessageProperties();
            System.out.println("Message content type: " + props.getContentType());
            
            if (props.getContentType() != null && 
                props.getContentType().contains("java-serialized-object")) {
                
                try {
                    // Create a custom ObjectInputStream that handles the missing class
                    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(message.getBody())) {
                        @Override
                        protected Class<?> resolveClass(java.io.ObjectStreamClass desc) 
                                throws java.io.IOException, ClassNotFoundException {
                            String name = desc.getName();
                            try {
                                return Class.forName(name);
                            } catch (ClassNotFoundException e) {
                                // Handle the case where the NotificationDTO class is missing
                                if (name.equals("ticketBookingSystem.dto.notification.NotificationDTO")) {
                                    System.out.println("Using HashMap for NotificationDTO");
                                    return HashMap.class; // Use HashMap as a substitute
                                }
                                throw e;
                            }
                        }
                    };
                    
                    // Read the object (will be deserialized as a HashMap due to our custom class loader)
                    Object obj = ois.readObject();
                    ois.close();
                    
                    System.out.println("Deserialized object: " + obj);
                    
                    // Extract the important fields from the deserialized HashMap
                    NotificationEvent event = new NotificationEvent();
                    
                    if (obj instanceof HashMap) {
                        // This is our deserialized NotificationDTO as a HashMap
                        HashMap<?, ?> map = (HashMap<?, ?>) obj;
                        
                        System.out.println("Deserialized HashMap contents:");
                        for (Object key : map.keySet()) {
                            System.out.println("Field: " + key + " = " + map.get(key));
                        }
                        
                        // Extract fields that exist in NotificationEvent
                        if (map.containsKey("message") && map.get("message") != null) {
                            event.setMessage(map.get("message").toString());
                        }
                        
                        if (map.containsKey("subject") && map.get("subject") != null) {
                            event.setSubject(map.get("subject").toString());
                        }
                        
                        if (map.containsKey("recipientEmailAddress") && map.get("recipientEmailAddress") != null) {
                            event.setRecipientEmailAddress(map.get("recipientEmailAddress").toString());
                        }
                    } else if (obj != null) {
                        // Try reflection as a fallback
                        System.out.println("Attempting reflection on object of type: " + obj.getClass());
                        try {
                            // Extract message field if it exists
                            try {
                                Field msgField = obj.getClass().getDeclaredField("message");
                                msgField.setAccessible(true);
                                Object messageContent = msgField.get(obj);
                                if (messageContent != null) {
                                    event.setMessage(messageContent.toString());
                                }
                            } catch (NoSuchFieldException e) {
                                System.out.println("No message field found");
                            }
                            
                            // Extract subject field if it exists
                            try {
                                Field subjectField = obj.getClass().getDeclaredField("subject");
                                subjectField.setAccessible(true);
                                Object subject = subjectField.get(obj);
                                if (subject != null) {
                                    event.setSubject(subject.toString());
                                }
                            } catch (NoSuchFieldException e) {
                                System.out.println("No subject field found");
                            }
                            
                            // Extract recipientEmailAddress field if it exists
                            try {
                                Field emailField = obj.getClass().getDeclaredField("recipientEmailAddress");
                                emailField.setAccessible(true);
                                Object email = emailField.get(obj);
                                if (email != null) {
                                    event.setRecipientEmailAddress(email.toString());
                                }
                            } catch (NoSuchFieldException e) {
                                System.out.println("No recipientEmailAddress field found");
                            }
                        } catch (Exception e) {
                            System.out.println("Reflection failed: " + e.getMessage());
                        }
                    }
                    
                    System.out.println("Converted to NotificationEvent: message=" + event.getMessage() + 
                                    ", subject=" + event.getSubject() + 
                                    ", email=" + event.getRecipientEmailAddress());
                    return event;
                    
                } catch (Exception e) {
                    System.err.println("Error deserializing Java object: " + e);
                    e.printStackTrace();
                    
                    // Fall back to the next approach
                }
            }
            
            // Try JSON conversion as a fallback
            try {
                return jsonConverter.fromMessage(message);
            } catch (Exception e) {
                System.err.println("Error deserializing as JSON: " + e.getMessage());
            }
            
            // Last resort, try simple message conversion
            return javaConverter.fromMessage(message);
        } catch (Exception e) {
            throw new MessageConversionException("Failed to convert message", e);
        }
    }
}