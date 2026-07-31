package pl.owntelecom.models;

import org.bukkit.entity.Player;

import java.util.UUID;

public class Call {

    public enum CallState {
        RINGING,    // Dzwoni
        ACTIVE,     // Aktywne połączenie
        ENDED       // Zakończone
    }

    private final UUID callId;
    private final Player caller;
    private final Player receiver;
    private CallState state;
    private final long startTime;
    private long endTime;
    private int messagesCount;
    private String callerOperatorId;
    private String receiverOperatorId;
    private double signalQuality;

    public Call(Player caller, Player receiver) {
        this.callId = UUID.randomUUID();
        this.caller = caller;
        this.receiver = receiver;
        this.state = CallState.RINGING;
        this.startTime = System.currentTimeMillis();
        this.messagesCount = 0;
        this.signalQuality = 1.0;
    }

    // Gettery
    public UUID getCallId() { return callId; }
    public Player getCaller() { return caller; }
    public Player getReceiver() { return receiver; }
    public CallState getState() { return state; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public int getMessagesCount() { return messagesCount; }
    public String getCallerOperatorId() { return callerOperatorId; }
    public String getReceiverOperatorId() { return receiverOperatorId; }
    public double getSignalQuality() { return signalQuality; }

    // Settery
    public void setState(CallState state) { this.state = state; }
    public void setCallerOperatorId(String id) { this.callerOperatorId = id; }
    public void setReceiverOperatorId(String id) { this.receiverOperatorId = id; }
    public void setSignalQuality(double quality) { this.signalQuality = quality; }

    public void incrementMessages() {
        this.messagesCount++;
    }

    public void endCall() {
        this.state = CallState.ENDED;
        this.endTime = System.currentTimeMillis();
    }

    public long getDurationSeconds() {
        long end = (state == CallState.ENDED) ? endTime : System.currentTimeMillis();
        return (end - startTime) / 1000;
    }

    public long getDurationMinutes() {
        return Math.max(1, (getDurationSeconds() + 59) / 60); // Zaokrąglanie w górę
    }

    public double getCallCost(double ratePerMinute) {
        return getDurationMinutes() * ratePerMinute;
    }

    public boolean isActive() {
        return state == CallState.ACTIVE || state == CallState.RINGING;
    }

    public Player getOtherParty(Player player) {
        return player.equals(caller) ? receiver : caller;
    }
}
