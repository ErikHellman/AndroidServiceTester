package se.hellsoft.servicetester;

interface IMyServiceInterface {
    int myPid();

    int[] startIds();

    int removeOldestStartId();

    void callStopSelf(int startId);
}
