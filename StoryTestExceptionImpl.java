package Solution;

import Provided.StoryTestException;

import java.util.LinkedList;
import java.util.List;

public class StoryTestExceptionImpl extends StoryTestException {
    public LinkedList<String> actualVals, expectedVals;
    int numFails;
    String firstThensentence;

    public StoryTestExceptionImpl(List<String> actual, List<String> expected, int num_fails, String sent) {
        actualVals = new LinkedList<String>();
        expectedVals = new LinkedList<String>();
        actualVals.addAll(actual);
        expectedVals.addAll(expected);
        numFails=num_fails;
        firstThensentence=sent;

    }

    @Override
    public String getSentence() {
        return firstThensentence;
    }

    @Override
    public List<String> getStoryExpected() {
        return expectedVals;
    }

    @Override
    public List<String> getTestResult() {
        return actualVals;
    }

    @Override
    public int getNumFail() {
        return numFails;
    }
}
