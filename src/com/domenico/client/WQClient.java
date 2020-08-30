package com.domenico.client;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.rmi.NotBoundException;

public class WQClient implements WQInterface {

    private final RMIClient rmiClient;
    private final TCPClient tcpClient;
    private final UDPClient udpClient;
    private final ChallengeListener challengeListener;  //listener used to signal the events relative to the challenge request
    private String loggedUserName;
    private int challengeLengthSec;
    private int challengeWords;
    private boolean playing;
    private int wordCounter;

    public WQClient(ChallengeListener listener) throws IOException, NotBoundException {
        this.loggedUserName = null;
        this.rmiClient = new RMIClient();
        this.tcpClient = new TCPClient(this);
        this.udpClient = new UDPClient(this);
        this.challengeListener = listener;
        new Thread(this.udpClient).start();
    }

    @Override
    public String onRegisterUser(String username, String password) throws Exception {
        if (!isLoggedIn()) {
            return rmiClient.register(username, password);
        } else {
            return Messages.LOGOUT_NEEDED;
        }
    }

    @Override
    public String onLogin(String username, String password) throws Exception {
        if (!isLoggedIn()) {
            String result = tcpClient.login(username, password, udpClient.getUDPPort());
            if (result != null && result.isEmpty()) {
                loggedUserName = username;
                return Messages.LOGIN_SUCCESS;
            }
            return result;
        } else {
            return Messages.LOGOUT_NEEDED;
        }
    }

    @Override
    public String onLogout() throws Exception {
        if (isLoggedIn()) {
            String result = tcpClient.logout(loggedUserName);
            if (result != null && result.isEmpty()) {
                loggedUserName = null;
                return Messages.LOGOUT_SUCCESS;
            } else {
                return result;
            }
        } else {
            return Messages.LOGIN_NEEDED;
        }
    }

    @Override
    public String onAddFriend(String friendUsername) throws Exception {
        if (isLoggedIn()) {
            return tcpClient.addFriend(loggedUserName, friendUsername);
        } else {
            return Messages.LOGIN_NEEDED;
        }
    }

    @Override
    public JSONArray onShowFriendList() throws Exception {
        if (isLoggedIn()) {
            return tcpClient.friendList(loggedUserName);
        } else {
            return null;
        }
    }

    @Override
    public String onShowScore() throws Exception {
        if (isLoggedIn()) {
            return tcpClient.showScore(loggedUserName);
        } else {
            return Messages.LOGIN_NEEDED;
        }
    }

    @Override
    public JSONObject onShowLeaderboard() throws Exception {
        if (isLoggedIn()) {
            return tcpClient.showLeaderboard(loggedUserName);
        } else {
            return null;
        }
    }

    @Override
    public String onSendChallengeRequest(String friendUsername) throws Exception {
        if (isLoggedIn()) {
            return tcpClient.sendChallengeRequest(loggedUserName, friendUsername);
        } else {
            return Messages.LOGIN_NEEDED;
        }
    }

    @Override
    public boolean waitChallengeResponse(StringBuffer response) throws Exception {
        boolean accepted = tcpClient.getChallengeResponse(response);
        playing = accepted;
        return accepted;
    }

    @Override
    public String onChallengeStart() throws Exception {
        String[] challengeSettings = new String[2];
        String nextItWord = tcpClient.onChallengeStart(challengeSettings);
        if (nextItWord.isEmpty()) return "";
        challengeLengthSec = Integer.parseUnsignedInt(challengeSettings[0]) / 1000;
        challengeWords = Integer.parseUnsignedInt(challengeSettings[1]);
        playing = true;
        wordCounter = 1;
        return nextItWord;
    }

    @Override
    public String getNextWord() throws Exception {
        wordCounter++;
        return tcpClient.getNextWord();
    }

    @Override
    public boolean sendTranslation(String enWord) {
        return false;
    }

    @Override
    public void onExit() {
        this.udpClient.stopProcessing();
        this.tcpClient.exit();
    }

    @Override
    public int getChallengeLength() { return challengeLengthSec; }

    @Override
    public int getChallengeWords() { return challengeWords; }

    @Override
    public boolean isLoggedIn() { return loggedUserName != null; }

    @Override
    public boolean isPlaying() { return playing; }

    @Override
    public int getWordCounter() { return wordCounter; }

    public void onChallengeTimeout() {
        this.wordCounter = 1;
        this.playing = false;
        challengeListener.onChallengeTimeout();
    }

    public void onChallengeArrived(String from) {
        challengeListener.onChallengeArrived(from, this::onChallengeResponse);
    }

    public void onChallengeResponse(Boolean accepted) {
        udpClient.onChallengeResponse(accepted);
        this.playing = accepted;
    }

    public void onChallengeArrivedTimeout() {
        this.playing = false;
        challengeListener.onChallengeArrivedTimeout();
    }
}
