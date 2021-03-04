package Solution;
import Provided.GivenNotFoundException;
import Provided.StoryTester;
import Provided.ThenNotFoundException;
import Provided.WhenNotFoundException;

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.LinkedList;
import org.junit.ComparisonFailure;

public class StoryTesterImpl implements StoryTester {
    @Override
    public void testOnNestedClasses(String story, Class<?> testClass) throws Exception {
        auxNested(story,testClass,null,null);
    }
    private void auxNested ( String story, Class<?> testClass, Class<?> parentClass, Object parentObject)
            throws IllegalArgumentException, GivenNotFoundException, WhenNotFoundException, ThenNotFoundException, IllegalAccessException, StoryTestExceptionImpl, InvocationTargetException, NoSuchMethodException, InstantiationException {
        if (story == null || testClass == null) {
            throw new IllegalArgumentException();
        }
        int numGivenException = 0;
        Class<?>[] nestedClasses = testClass.getDeclaredClasses();
        Object o = ( parentClass != null )? makeObjectWhenParentExists(testClass,parentClass, parentObject) : makeObj(testClass);
        //Object o1 =  makeObj(testClass);

        try {
            //split story into sentences/lines
            String[] linesOfStory = splitStoryIntoLines(story);
            //given sentence without the word given at index 1 in the array
            String[] givenSentenceWithoutGivenAt1 = splitFirstWordAndRestOfSentence(linesOfStory[0]);

            //get all parameters in sentence of type2
            Object[] params = getParamsSplittedByAnd(givenSentenceWithoutGivenAt1[1]);
            //get the wanted method
            Method method = givenMethodFind(testClass, givenSentenceWithoutGivenAt1[1]);
            // make the method accessible
            method.setAccessible(true);



            method.invoke(o, params);
            //finished the given function that means setting values of fields

            //============================
            testWhensAndThens(o, testClass, story);
        } catch (GivenNotFoundException e) {
            for (Class<?> c : nestedClasses) {
                try {
                    auxNested(story, c,testClass, o);
                } catch (GivenNotFoundException e1) {
                    numGivenException++;
                }
                if (nestedClasses.length == numGivenException)throw new GivenNotFoundException();
            }
        }


    }
    private Object makeObjectWhenParentExists(Class<?> testClass,Class<?> parentClass, Object parentObject)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
            //if ( parentClass == null ) return makeObj(testClass);
            Constructor<?> ctor = testClass.getDeclaredConstructor(parentClass);
            ctor.setAccessible(true);
            //return (ctor.newInstance(makeObjectWhenParentExists(testClass,null)));
            if (parentObject == null) return (ctor.newInstance(parentClass.newInstance()));
            return (ctor.newInstance(parentObject));



    }
    @Override
    public void testOnInheritanceTree(String story, Class<?> testClass) throws Exception {
        if (story == null || testClass == null) throw new IllegalArgumentException();

        //split story into sentences/lines
        String[] linesOfStory = splitStoryIntoLines(story);
        //given sentence without the word given at index 1 in the array
        String[] givenSentenceWithoutGivenAt1 = splitFirstWordAndRestOfSentence(linesOfStory[0]);

        startTestGiven( story,testClass,givenSentenceWithoutGivenAt1);
    }



    private void startTestGiven(String story, Class<?> testClass, String[] givenSentenceWithoutGivenAt1 ) throws Exception {
        //get the wanted method
        Method method = givenMethodFind(testClass, givenSentenceWithoutGivenAt1[1]);

        Object object = makeObj(testClass);
        //get all parameters in sentence of type2
        Object[] params = getParamsSplittedByAnd(givenSentenceWithoutGivenAt1[1]);
        // make the method accessible
        method.setAccessible(true);
        method.invoke(object,params);
        //finished the given function that means setting values of fields

        //============================
        testWhensAndThens(object, testClass, story);
    }

    private String[] splitFirstWordAndRestOfSentence(String line) {
        return line.split(" ", 2);
    }

    private String[] splitStoryIntoLines(String story) {
        return story.split("\n");
    }

    private Method givenMethodFind(Class<?> testClass, String givenSentenceWithoutGiven)
            throws GivenNotFoundException {
        if (testClass == null) throw new GivenNotFoundException();
        //get class methods
        Method[] methods = testClass.getDeclaredMethods();
        //check which method is with the same annotation and return it if found
        //and make sure the sentence saved in the annotation is equal to the one searched for.
        for (Method method : methods) {
            method.setAccessible(true);

            if (method.isAnnotationPresent(Given.class)) {
                Class<?>[] params = method.getParameterTypes();

                //returns annotation sentence in the current method
                String annoSentence = method.getAnnotation(Given.class).value();


                String[] methodSentencesSplitted = splitbyAND(annoSentence);
                String[] givenSentencesSplitted = splitbyAND(givenSentenceWithoutGiven);

                int lengthOfMethodSplitted = methodSentencesSplitted.length;
                int lengthOfGivenSplitted = givenSentencesSplitted.length;
                //check if this is the method with the same sentence and parameters

                if ( (lengthOfMethodSplitted == lengthOfGivenSplitted)
                        && annoIsRightInAndSplittedArray(methodSentencesSplitted, givenSentencesSplitted)
                        )
                    return method;
            }
        }
        //if method is not found in current class search in superclass
        return givenMethodFind(testClass.getSuperclass(), givenSentenceWithoutGiven);
    }

    // returns true if the annotation sentence is the same as the sentence received
    //check if this is the method with the same sentence and parameters
    private boolean annoIsRightInAndSplittedArray(String[] methodSentencesSplitted,
                                                  String[] givenSentencesSplitted) {

        for (int index = 0; index < methodSentencesSplitted.length; ++index) {
            String paramOfGivenSentence = getParamInSent(givenSentencesSplitted[index]);

            // split the sentence into array: givenParam, at index 0 there is the sentence
            //at index 1 there is the parameter, remember we want to check if the sentence in the method annotation is the same
            //as the method in the
            String[] givenSentenceAt0 = splitByParamAtIndex0FindSentence(givenSentencesSplitted[index], paramOfGivenSentence);

            //split the sentence in the method annotation into same as above save in array: splitMethodSentToSentNdParam
            String[] methodSentenceAt0 = splitByParamAtIndex0FindSentence(methodSentencesSplitted[index], "&");


            //if sentence in annotations isnt equal to the given one return false
            if (!(givenSentenceAt0[0].equals(methodSentenceAt0[0]))) return false;

        }
        return true;
    }

    //=============
    private Object[] getParamsSplittedByAnd(String s) {
        String[] sentencesOfType1 = s.split(" and ");
        Object[] parameters = new Object[sentencesOfType1.length];
        for (int i = 0; i < sentencesOfType1.length; ++i) {
            String tmpPara = getParamInSent(sentencesOfType1[i]);
            try {
                parameters[i] = Integer.valueOf(tmpPara);
            } catch (NumberFormatException e) {
                parameters[i] = tmpPara;
            }
        }



        return parameters;
    }

    private Object makeObj(Class<?> testClass) //Object instance, Class<?> parent)
            throws NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
        Constructor<?> cons = testClass.getDeclaredConstructor();
        cons.setAccessible(true);
        return (cons.newInstance());

    }

    private String getParamInSent(String s) {
        return s.substring(s.lastIndexOf(" ") + 1);
    }

    private String[] splitByParamAtIndex0FindSentence(String s, String param) {
        return s.split(param);
    }

    private String[] splitbyAND(String s) {
        return s.split(" and");
    }


    //word- then or when
    private void testWhensAndThens(Object o, Class<?> testClass, String story)
            throws InvocationTargetException, IllegalAccessException, WhenNotFoundException, ThenNotFoundException, StoryTestExceptionImpl {
        // argument is true if "when" is expected, because when comes before "then"
        int expectingWhenIs1ExpectingThen0 = 1;
        String emptyString="";
        String[] lines = splitStoryIntoLines(story);
        //indicates how many tests failed, if any test failed then return an exception
        int numTestsFailed = 0;
        //create a map to save the backup of the fields of the object
        HashMap<Field, Object> bkUpDir = null;


        boolean notFirstThenFailed = false;
        boolean thenWorked = false;
        String firstThenFailSentence = emptyString;
        int flagError = 0;
        //create lists for exception usage
        LinkedList<String> actualList = new LinkedList<String>();
        LinkedList<String> expectedList = new LinkedList<String>();

        // FOR EACH LINE IN THE STORY
        for (String sentence : lines) {
            //GET FIRST WORD AND CHECK WHICH KEYWORD IT IS
            String[] firstWordAt0RestAt1 = splitFirstWordAndRestOfSentence(sentence);
            String firstWord = firstWordAt0RestAt1[0];
            //SAVE THE REST OF THE SENTENCE FOR FUTURE USE
            String restOfSentence = firstWordAt0RestAt1[1];
            //=====================================================
            //if (numTestsFailed != 0) break;
            thenWorked = false;
            if (firstWord.equals("When")) {

                // IF FIRST WORD IS WHEN GET THE PARAMETERS USING restOfSentence  AND GET
                //THE WANTED METHOD
                Object[] params = getParamsSplittedByAnd(restOfSentence);
                Method method = whenMethodFind(testClass, restOfSentence);
                // MAKE SURE THAT WHEN COMES BEFORE THEN
                if (expectingWhenIs1ExpectingThen0 == 1) {
                    //NOW EXPECTING THEN
                    expectingWhenIs1ExpectingThen0 = 0;
                    //CREATE A BACKUP FOR THE CURRENT OBJECT
                    bkUpDir = CreateBackupOfObject(testClass, o);
                }
                // NOW INVOKE WANTED METHOD WE FOUND
                method.setAccessible(true);
                method.invoke(o, params);
                //======================================================
            } else if (firstWord.equals("Then")) {
                int numThens = 0;
                //cut then sentence into parts that are splitted by or, we call them thenParts
                String[] thenParts = restOfSentence.split(" or ");
                //
                if (expectingWhenIs1ExpectingThen0 == 0) expectingWhenIs1ExpectingThen0 = 1;

                // for each type2 sentence between the or-s in the then sentence
                for (String partOfOrInThen : thenParts) {
                    if (thenWorked) break;


                    try {

                        Object[] params = getParamsSplittedByAnd(partOfOrInThen);
                        Method method = thenMethodFind(testClass, partOfOrInThen);
                        method.setAccessible(true);
                        method.invoke(o, params);
                        thenWorked = true;
                        numThens++;
                        notFirstThenFailed = false;
                        actualList = new LinkedList<String>();
                        expectedList = new LinkedList<String>();
                        flagError = 0;


                    } catch (InvocationTargetException e) {

                        numThens++;
                        ComparisonFailure tarEx = (ComparisonFailure) e.getTargetException();
                        flagError = 1;
                        if (!notFirstThenFailed) {

                            actualList.addLast(tarEx.getActual());
                            expectedList.addLast(tarEx.getExpected());
                        }


                    }

                }
                int numOfThensInCurrentLine = thenParts.length;
                if( flagError == 1  && !notFirstThenFailed) {
                    notFirstThenFailed = true;
                }
                // if first then in the line didn't work then we need to add the to the exception object
                if (flagError == 1) {
                    // if num of then parts equal to the num of thens that failed then we failed all thens in line
                    //therefore, the test for this line will fail
                    flagError = 0;

                    if (thenWorked) break;
                    if (numOfThensInCurrentLine == numThens) {
                        numTestsFailed++;
                        //return old fields from backup to object
                        restoreBackupToObject(testClass, o, bkUpDir);

                        //first then fail sentence for exception usage
                        firstThenFailSentence = sentence;

                    } else {
                        //then didn't fail, go to the next line, empty the lists

                    }
                }

            }

        }
        if (numTestsFailed != 0)
            throw new StoryTestExceptionImpl(actualList, expectedList, numTestsFailed, firstThenFailSentence);
    }




    private HashMap<Field, Object> CreateBackupOfObject(Class<?> testClass, Object object) {
        Field[] fieldsOfClass = testClass.getDeclaredFields();
        HashMap<Field, Object> directory = new HashMap<Field, Object>();
        // for each field copy its object, firstly check if clone if it is then copy as clone
        //if not check if it has a copy constructor if it does then copy by constructor, if not save the object itself.
        for (Field field : fieldsOfClass) {

            field.setAccessible(true);
            try {
                Object toSave = field.get(object);
                // check if it implements cloneable
                if (toSave instanceof Cloneable) {


                    Method wantedClone = field.getType().getMethod("clone");
                    directory.put(field, wantedClone.invoke(toSave));

                } else {

                    // check if it has a copy constructor
                    Constructor<?> copyConstructor = field.getType().getDeclaredConstructor(field.getType());
                    copyConstructor.setAccessible(true);
                    directory.put(field, copyConstructor.newInstance(toSave));
                }
                // if none of the above save the object itself
            } catch (IllegalAccessException e) {
                try {
                    directory.put(field, field.get(object));
                } catch (IllegalAccessException e1) {

                }
                ;
            } catch (InvocationTargetException e) {
                try {
                    directory.put(field, field.get(object));
                } catch (IllegalAccessException e1) {
                }
                ;
            } catch (InstantiationException e) {
                try {
                    directory.put(field, field.get(object));
                } catch (IllegalAccessException e1) {
                }
                ;
            } catch (NoSuchMethodException e) {
                try {
                    directory.put(field, field.get(object));
                } catch (IllegalAccessException e1) {

                }

            }

        }
        return directory;
    }

    private void restoreBackupToObject(Class<?> testClass, Object o, HashMap<Field, Object> backUp) {
        Field[] fieldsOfClass = testClass.getDeclaredFields();
        for (Field field : fieldsOfClass) {
            try {
                field.setAccessible(true);
                Object fieldObject = field.get(o);
                Object objectToRestore = backUp.get(field);
                if (fieldObject instanceof Cloneable) {
                    Method cloneFuncOfField = field.getType().getMethod("clone");
                    field.set(o, cloneFuncOfField.invoke(objectToRestore));
                } else {
                    Constructor<?> copyConstructor = field.getType().getDeclaredConstructor(field.getType());
                    copyConstructor.setAccessible(true);
                    field.set(o, copyConstructor.newInstance(objectToRestore));

                }
            } catch (IllegalAccessException e) {
                try {
                    field.set(o, backUp.get(field));
                } catch (IllegalAccessException e1) {

                }

            } catch (InvocationTargetException e) {
                try {
                    field.set(o, backUp.get(field));
                } catch (IllegalAccessException e1) {
                }
                ;
            } catch (InstantiationException e) {
                try {
                    field.set(o, backUp.get(field));
                } catch (IllegalAccessException e1) {
                }
                ;
            } catch (NoSuchMethodException e) {
                try {
                    field.set(o, backUp.get(field));
                } catch (IllegalAccessException e1) {

                }

            }
        }
    }



    private  Method whenMethodFind(Class<?> testClass, String whenSentenceWithoutWhen)
            throws WhenNotFoundException {
        if (testClass == null) throw new WhenNotFoundException();
        //get class methods
        Method[] methods = testClass.getDeclaredMethods();
        //check which method is with the same annotation and return it if found
        //and make sure the sentence saved in the annotation is equal to the one searched for.
        for (Method method : methods) {
            method.setAccessible(true);

            if (method.isAnnotationPresent(When.class)) {
                Class<?>[] params = method.getParameterTypes();

                //returns annotation sentence in the current method
                String annoSentence = method.getAnnotation(When.class).value();


                String[] methodSentencesSplitted = splitbyAND(annoSentence);
                String[] givenSentencesSplitted = splitbyAND(whenSentenceWithoutWhen);
                int lengthOfMethodSplitted = methodSentencesSplitted.length;
                int lengthOfGivenSplitted = givenSentencesSplitted.length;
                //check if this is the method with the same sentence and parameters

                    if ( (lengthOfMethodSplitted == lengthOfGivenSplitted)
                            && annoIsRightInAndSplittedArray( methodSentencesSplitted, givenSentencesSplitted)
                            )
                        return method;

            }
        }
        //if method is not found in current class search in superclass
        return whenMethodFind(testClass.getSuperclass(), whenSentenceWithoutWhen);
    }

    private  Method thenMethodFind(Class<?> testClass, String thenSentenceWithoutThen)
            throws ThenNotFoundException {

        if (testClass == null) throw new ThenNotFoundException();
        //get class methods
        Method[] methods = testClass.getDeclaredMethods();
        //check which method is with the same annotation and return it if found
        //and make sure the sentence saved in the annotation is equal to the one searched for.
        for (Method method : methods) {
            method.setAccessible(true);

            if (method.isAnnotationPresent(Then.class)) {
                Class<?>[] params = method.getParameterTypes();

                //returns annotation sentence in the current method
                String annoSentence = method.getAnnotation(Then.class).value();


                String[] methodSentencesSplitted = splitbyAND(annoSentence);
                String[] givenSentencesSplitted = splitbyAND(thenSentenceWithoutThen);
                int lengthOfMethodSplitted = methodSentencesSplitted.length;
                int lengthOfGivenSplitted = givenSentencesSplitted.length;
                //check if this is the method with the same sentence and parameters

                if ( (lengthOfMethodSplitted == lengthOfGivenSplitted)
                        && annoIsRightInAndSplittedArray( methodSentencesSplitted, givenSentencesSplitted)
                        )
                    return method;
            }
        }
       // Class<?> fordebug = testClass.getSuperclass();
        //if method is not found in current class search in superclass
        return thenMethodFind(testClass.getSuperclass(), thenSentenceWithoutThen);
    }

}