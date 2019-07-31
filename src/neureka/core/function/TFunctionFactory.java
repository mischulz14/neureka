package neureka.core.function;

import neureka.core.function.imp.Constant;
import neureka.core.function.imp.Input;
import neureka.core.function.util.Construction;
import neureka.core.function.util.Context;

import java.util.*;

public class TFunctionFactory {
    /*
	     0:  ReLu;
		 1:  Sigmoid;
		 2:  Tanh;
		 3:  Quadratic;
		 4:  Ligmoid;
		 5:  Linear;
		 6:  Gaussian;
		 7:  abs;
		 8:  sin;
		 9:  cos;
		 10: sum;
		 11: prod;
		 12: ^;
		 13: /;
		 14: *;
		 15: %;
		 16: -;
		 17: +;
		 18: x; (conv)
	 */
    //=================================================================================================

    public static TFunction newBuild(int f_id, int size, boolean doAD){
        if (f_id == 18){
            size = 2;
        }
        if (f_id < 10) {
            return newBuild(Context.REGISTER[f_id] + "(I[0])", doAD);//, tipReached);
        } else if (f_id < 12) {
            return newBuild(Context.REGISTER[f_id] + "I[j]", doAD);//, tipReached);
        } else {
            String expression = "I[0]";
            for (int i = 0; i < size - 1; i++) {
                expression += Context.REGISTER[f_id] + "I[" + (i + 1) + "]";
            }
            return newBuild(expression, doAD);
        }
    }

    public static TFunction newBuild(String expression, boolean doAD) {
        HashMap<String, TFunction> map = Context.SHARED;
        expression =
            (expression.length()>0
                    && (expression.charAt(0) != '('||expression.charAt(expression.length() - 1) != ')'))
                        ? ("(" + expression + ")")
                        : expression;
        String k = (doAD)?"d"+expression:expression;
        if (Context.SHARED.containsKey(k)) {
            return Context.SHARED.get(k);
        }
        TFunction built = construct(expression, doAD);//, tipReached);
        if (built != null) {
            Context.SHARED.put(((doAD)?"d":"")+"(" + built.toString() + ")", built);
        }
        return built;
    }

    private static TFunction construct(String expression, boolean doAD){
        TFunction function = null;
        ArrayList<TFunction> sources = new ArrayList<>();;
        if (expression == null) {
            expression = "";
        }
        if (expression == "") {
            TFunction newCore = new Constant();
            newCore = newCore.newBuild("0");
            return newCore;
        }
        System.out.println("Equation packed: " + expression);
        expression = utility.unpackAndCorrect(expression);
        System.out.println("Equation unpacked: " + expression);
        List<String> Operations = new ArrayList<String>();
        List<String> Components = new ArrayList<String>();
        int i = 0;
        System.out.println("Expression length : " + expression.length());
        while (i < expression.length()) {
            System.out.println("\nPARSING [" + expression + "] at i:" + i + "\n================");
            System.out.println("parsing at " + i + " => searching for component!");
            final String newComponent = utility.parsedComponent(expression, i);
            if (newComponent != null) {
                System.out.println("Component found! : " + newComponent + " ");
                System.out.println(Components.size() + " < " + Operations.size() + " ?: true -> Components.e_add(" + newComponent + ")");
                if (Components.size() <= Operations.size()) {
                    Components.add(newComponent);
                }
                i += newComponent.length();
                System.out.println("parsing at " + i + " => searching for operation!");
                final String newOperation = utility.parsedOperation(expression, i);
                if (newOperation != null) {
                    System.out.println("Operation found! : " + newOperation + " ");
                    i += newOperation.length();
                    if (newOperation.length() <= 0) {
                        continue;
                    }
                    Operations.add(newOperation);
                }
                System.out.println("parsed operation: " + newOperation);
            } else {
                ++i;
            }
            System.out.println("Ending iteration with i:" + i + "\nExpression: " + expression + "\n=============================\n----------------------------");
            Components.forEach((c) -> {
                System.out.print("[" + c + "]");
            });
            System.out.print(";\n");
            Operations.forEach((o) -> {
                System.out.print("[" + o + "]");
            });
            System.out.print(";\n");
        }
        System.out.println("Components and operations parsed: " + Components);
        System.out.println("Operations and components parsed: " + Operations);
        //===
        int Count = Context.REGISTER.length;
        for (int j = Context.REGISTER.length; j > 0; --j) {
            if (!utility.containsOperation(Context.REGISTER[j - 1], Operations)) {
                --Count;
            } else {
                j = 0;
            }
        }
        int ID = 0;
        while (ID < Count) {
            final List<String> newOperations = new ArrayList<String>();
            final List<String> newComponents = new ArrayList<String>();
            System.out.println("current (" + Context.REGISTER[ID] + ") and most low rank operation: " + Context.REGISTER[Count - 1]);
            if (utility.containsOperation(Context.REGISTER[ID], Operations)) {
                String currentChain = null;
                boolean groupingOccured = false;
                boolean enoughtPresent = utility.numberOfOperationsWithin(Operations) > 1;// Otherwise: I[j]^4 goes nuts!
                if (enoughtPresent) {
                    String[] ComponentsArray = Components.toArray(new String[0]);
                    int length = ComponentsArray.length;
                    System.out.println("Iterating over " + length + " components: ");
                    for (int Ci = 0; Ci < length; Ci++) {
                        System.out.println("\nIteration " + Ci + "/" + length);
                        System.out.println("====================");
                        String currentComponent = null;
                        currentComponent = ComponentsArray[Ci];
                        String currentOperation = null;
                        if (Operations.size() > Ci) {
                            currentOperation = Operations.get(Ci);
                        }
                        System.out.println("Component: " + currentComponent);
                        System.out.println("Operation: " + currentOperation);
                        System.out.println("Chain: " + currentChain);
                        if (currentOperation != null) {
                            if (currentOperation.equals(Context.REGISTER[ID])) {
                                final String newChain =
                                        utility.groupBy(Context.REGISTER[ID], currentChain, currentComponent, currentOperation);
                                System.out.println("newChain: " + newChain);
                                if (newChain != null) {
                                    currentChain = newChain;
                                }
                                System.out.println("This needed to be grouped");
                                groupingOccured = true;
                            } else {
                                if (currentChain == null) {
                                    newComponents.add(currentComponent);
                                } else if (currentChain != null) {
                                    newComponents.add(currentChain + currentComponent); //= String.valueOf(currentChain) + currentComponent
                                }
                                newOperations.add(currentOperation);
                                groupingOccured = true;
                                currentChain = null;
                            }
                        } else {
                            if (currentChain == null) {
                                newComponents.add(currentComponent);
                            } else {
                                newComponents.add(currentChain + currentComponent);
                                groupingOccured = true;
                            }
                            currentChain = null;
                        }
                        System.out.println("----------------\nComponent: " + currentComponent);
                        System.out.println("Operation: " + currentOperation);
                        System.out.println("Chain: " + currentChain + "\n");
                    }
                }
                if (groupingOccured) {
                    Operations = newOperations;
                    Components = newComponents;
                    System.out.println("Grouping occured:");
                } else {
                    System.out.println("Grouping did not occure:");
                }
            }
            System.out.println("ID:" + ID + " => Context.REGISTER[ID]: " + Context.REGISTER[ID]);
            System.out.println("Operations: " + Operations);
            System.out.println("Components: " + Components);
            ++ID;
        }//closed while(ID < Count)
        System.out.println("==========================================================================================================");
        //---------------------------------------------------------
        // identifying function id:
        int f_id = 0;
        if (Operations.size() >= 1) {
            System.out.println("Operation 0 : " + Operations.get(0));
            for (int k = 0; k < Context.REGISTER.length; ++k) {
                if (Context.REGISTER[k].equals(Operations.get(0))) {
                    f_id = k;
                }
            }
        }
        // building sources and function:
        if (Components.size() == 1) {
            System.out.println("Only one component left -> no operations! -> testing for function:");
            System.out.println("parsing at " + 0 + " ...(possibly a function!)");
            String possibleFunction = utility.parsedOperation(Components.get(0).toLowerCase(), 0);
            System.out.println("TFunction ?: " + possibleFunction);
            if (possibleFunction != null) {
                if (possibleFunction.length() > 1) {
                    for (int Oi = 0; Oi < Context.REGISTER.length; Oi++) {
                        if (Context.REGISTER[Oi].equals(possibleFunction)) {
                            f_id = Oi;
                            TFunction newCore = new TFunctionFactory()
                                .newBuild(
                                    utility.parsedComponent(Components.get(0), possibleFunction.length()), doAD
                                );
                            sources.add(newCore);
                            function = Construction.createFunction(f_id, sources, doAD);
                            return function;
                            // Hallo mein Schatz! Ich bin`s, die Emi :) Ich liebe dich <3
                        }
                    }
                }
            }
            //function = Construction.createFunction(f_id, sources);
            System.out.println("TFunction: " + possibleFunction);
            //---
            System.out.println("1 comonent left: -> unpackAndCorrect(component)");
            String component = utility.unpackAndCorrect(Components.get(0));
            System.out.println("component: " + component);
            System.out.println("Checking if component is variable (value/input): ");
            if ((component.charAt(0) <= '9' && component.charAt(0) >= '0') || component.charAt(0) == '-' || component.charAt(0) == '+') {
                TFunction newFunction = new Constant();
                newFunction = newFunction.newBuild(component);
                System.out.println("is value leave! -> return newValueLeave.newBuilt(component)");
                return newFunction;
            }
            if (component.charAt(0) == 'i' || component.charAt(0) == 'I' ||
                    (component.contains("[") && component.contains("]") && component.matches(".[0-9]+."))) {//TODO: Make this regex better!!
                TFunction newFunction = new Input();
                newFunction = newFunction.newBuild(component);
                System.out.println("value leave! -> return newInputLeave.newBuilt(component)");
                return newFunction;
            }
            System.out.println("Component is not f f_id Leave! -> component = utility.cleanedHeadAndTail(component); ");
            component = utility.cleanedHeadAndTail(component);//If the component did not trigger variable creation: =>Cleaning!
            TFunction newBuild;
            System.out.println("new build: TFunction newBuild = (TFunction)new TFunctionFactory();");
            System.out.println("newBuild = newBuild.newBuild(component);");
            newBuild = new TFunctionFactory().newBuild(component, doAD);
            System.out.println("-> return newBuild;");
            return newBuild;
        } else {// More than one component left:
            if (Context.REGISTER[f_id] == "x") {
                Components = rebindPairwise(Components, f_id);
            }else if(Context.REGISTER[f_id] == ","){
                if(Components.get(0).startsWith("[")){
                    Components.set(0,Components.get(0).substring(1));
                    String[] splitted;
                    if(Components.get(Components.size()-1).contains("]")){
                        int offset = 1;
                        if(Components.get(Components.size()-1).contains("]:")){
                            offset = 2;
                            splitted = Components.get(Components.size()-1).split("]:");
                        }else{
                            splitted = Components.get(Components.size()-1).split("]");
                        }

                        if(splitted.length>1){
                            splitted = new String[]{splitted[0], Components.get(Components.size()-1).substring(splitted[0].length()+offset)};
                            Components.remove(Components.size()-1);
                            for(String part : splitted){
                                Components.add(part);
                            }
                        }
                    }
                }
            }
            final ListIterator<String> ComponentIterator2 = Components.listIterator();
            while (ComponentIterator2.hasNext()) {
                final String currentComponent2 = ComponentIterator2.next();
                System.out.println("this.Input.add(newCore2.newBuild(" + currentComponent2 + ")); Input.size(): " + sources.size());
                TFunction newCore2 = new TFunctionFactory().newBuild(currentComponent2, doAD);//Dangerous recursion lives here!
                sources.add(newCore2);
                if (newCore2 != null) {
                    System.out.println("newCore2 != null");
                }
            }
            sources.trimToSize();
            if (sources.size() == 1) {
                return sources.get(0);
            }
            if (sources.size() == 0) {
                return null;
            }
            ArrayList<TFunction> newVariable = new ArrayList<TFunction>();
            for (int Vi = 0; Vi < sources.size(); Vi++) {
                if (sources.get(Vi) != null) {
                    newVariable.add(sources.get(Vi));
                }
            }
            sources = newVariable;
            function = Construction.createFunction(f_id, sources, doAD);
            return function;
        }
    }

    private static List<String> rebindPairwise(List<String> components, int f_id) {
        if (components.size() > 2) {
            String newComponent = "(" + components.get(0) + Context.REGISTER[f_id] + components.get(1) + ")";
            components.remove(components.get(0));
            components.remove(components.get(0));
            components.add(0, newComponent);
            components = rebindPairwise(components, f_id);
        }
        return components;
    }
    //============================================================================================================================================================================================

    /**
     * UTILITY
     * for parsing:
     */
    private static class utility
    {
        public static int numberOfOperationsWithin(final List<String> operations) {
            int Count = 0;
            for (int i = 0; i < Context.REGISTER.length; ++i) {
                if (utility.containsOperation(Context.REGISTER[i], operations)) {
                    ++Count;
                }
            }
            return Count;
        }

        public static String parsedOperation(final String exp, final int i) {
            if (exp.length() <= i) {
                return null;
            }
            String operation = "";
            for (int Si = i; Si < exp.length(); ++Si) {
                operation = (operation) + exp.charAt(Si);
                if (utility.isBasicOperation(operation)) {
                    return operation;
                }
            }
            return null;
        }

        public static String parsedComponent(String exp, final int i) {
            exp = exp.trim();
            if (exp.length() <= i) {
                return null;
            }
            String component = "";
            int bracketDepth = 0;
            component = "";
            System.out.print("Start char: " + exp.charAt(i) + "\n");
            for (int Ei = i; Ei < exp.length(); ++Ei) {
                if (exp.charAt(Ei) == ')') {
                    --bracketDepth;
                } else if (exp.charAt(Ei) == '(') {
                    ++bracketDepth;
                }
                System.out.print("d[" + bracketDepth + "]:[  " + exp.charAt(Ei) + "  ], ");
                if (bracketDepth == 0) {
                    String possibleOperation = "";
                    for (int Sii = Ei + 1; Sii < exp.length(); ++Sii) {
                        possibleOperation = possibleOperation + exp.charAt(Sii);
                        if (utility.isBasicOperation(possibleOperation)) {
                            component = component + exp.charAt(Ei);
                            System.out.print("\n");
                            return component;
                        }
                    }
                }
                component += exp.charAt(Ei);
            }
            System.out.print("\n");
            return component;
        }

        public static boolean containsOperation(final String operation, final List<String> operations) {
            final ListIterator<String> OperationIterator = operations.listIterator();
            while (OperationIterator.hasNext()) {
                final String currentOperation = OperationIterator.next();
                if (currentOperation.equals(operation)) {
                    return true;
                }
            }
            return false;
        }

        public static boolean isBasicOperation(final String operation) {
            if (operation.length() > 8) {
                return false;
            }
            for (int i = 0; i < Context.REGISTER.length; ++i) {
                System.out.print(Context.REGISTER[i] + " =?= " + operation + " -:|:- ");
                if (Context.REGISTER[i].equals(operation)) {
                    System.out.println("");
                    return true;
                }
            }
            return false;
        }

        public static String groupBy(final String operation, final String currentChain, final String currentComponent, final String currentOperation) {
            String group = null;
            if (currentOperation != null) {
                if (currentOperation.equals(operation)) {
                    group = currentComponent + currentOperation;
                    if (currentChain != null) {
                        group = currentChain + group;
                    }
                }
            } else if (currentChain != null) {
                group = currentChain + currentComponent;
            }
            return group;
        }

        private static boolean isWeired(char c) {
            if (c == '"') {
                return true;
            }
            if ('�' == c) {
                return true;
            }
            if (c == '$') {
                return true;
            }
            if (c == '%') {
                return true;
            }
            if (c == '&') {
                return true;
            }
            if (c == '=') {
                return true;
            }
            if (c == '#') {
                return true;
            }
            if (c == '|') {
                return true;
            }
            if (c == '~') {
                return true;
            }
            if (c == ':') {
                return true;
            }
            if (c == ';') {
                return true;
            }
            if (c == '@') {
                return true;
            }
            if (c == '�') {
                return true;
            }
            if (c == '�') {
                return true;
            }
            if (c == '�') {
                return true;
            }
            if (c == '?') {
                return true;
            }
            if (c == '�') {
                return true;
            }
            if (c == '\\') {
                return true;
            }
            if (c == '>') {
                return true;
            }
            if (c == '<') {
                return true;
            }
            if (c == ' ') {
                return true;
            }
            return false;
        }

        public static String removeHeadAndTail(final String exp) {
            String corrected = "";
            for (int i = 1; i < exp.length() - 1; ++i) {
                corrected = corrected + exp.charAt(i);
            }
            return corrected;
        }

        public static String cleanedHeadAndTail(String exp) {
            exp = exp.trim();
            System.out.println("Unclean component: " + exp);
            int Ci = 0;
            String Updated = "";
            boolean condition = true;
            while (condition) {
                if (utility.isWeired(exp.charAt(Ci)) || (exp.charAt(Ci) >= 'A' && exp.charAt(Ci) <= 'Z') || (exp.charAt(Ci) >= 'a' && exp.charAt(Ci) <= 'z')) {
                    System.out.print("C: " + exp.charAt(Ci) + "; ");
                    Ci++;
                } else {
                    condition = false;
                }
                if (Ci == exp.length()) {
                    condition = false;
                }
            }
            for (int Gi = Ci; Gi < exp.length(); Gi++) {
                Updated += exp.charAt(Gi);
            }
            exp = Updated;
            Updated = "";
            System.out.print("\nUpdated: " + exp + "  \n");
            if (exp.length() > 0) {
                Ci = 0;
                condition = true;
                int l = exp.length() - 1;
                while (condition) {
                    if (utility.isWeired(exp.charAt(Ci)) || (exp.charAt(l - Ci) >= 'A' && exp.charAt(l - Ci) <= 'Z') || (exp.charAt(l - Ci) >= 'a' && exp.charAt(l - Ci) <= 'z')) {
                        System.out.print("C: " + exp.charAt(l - Ci) + "; ");
                        Ci++;
                    } else {
                        condition = false;
                    }
                    if (l - Ci < 0) {
                        condition = false;
                    }
                }
                for (int Gi = 0; Gi <= l - Ci; Gi++) {
                    Updated += exp.charAt(Gi);
                }
                exp = Updated;
            }
            if (exp.length() > 0) {
                if (exp.charAt(0) == '(' && exp.charAt(exp.length() - 1) != ')') {
                    exp = utility.removeHeadAndTail(exp);
                }
                if (exp.charAt(exp.length() - 1) == ')' && exp.charAt(0) != '(') {
                    exp = utility.removeHeadAndTail(exp);
                }
            }
            System.out.println("Cleaned component: " + exp);
            exp = exp.trim();
            return exp;
        }

        public static String unpackAndCorrect(String exp) {
            if (exp == null) {
                return null;
            }
            if (exp.length() == 0) {
                return "";
            }
            if (exp.equals("()")) {
                return "";
            }
            exp = exp.replace("sigmoid", Context.REGISTER[1]);
            exp = exp.replace("quadratic", Context.REGISTER[3]);
            exp = exp.replace("quadr", Context.REGISTER[3]);
            exp = exp.replace("lig", Context.REGISTER[4]);
            exp = exp.replace("ligmoid", Context.REGISTER[4]);
            exp = exp.replace("softplus", Context.REGISTER[4]);
            exp = exp.replace("spls", Context.REGISTER[4]);
            exp = exp.replace("ligm", Context.REGISTER[4]);
            exp = exp.replace("linear", Context.REGISTER[5]);
            exp = exp.replace("gaussian", Context.REGISTER[6]);
            exp = exp.replace("gauss", Context.REGISTER[6]);
            exp = exp.replace("absolute", Context.REGISTER[7]);
            exp = exp.replace("summation", Context.REGISTER[10]);
            exp = exp.replace("product", Context.REGISTER[11]);

            int bracketDepth = 0;
            for (int Ei = 0; Ei < exp.length(); ++Ei) {
                if (exp.charAt(Ei) == ')') {
                    --bracketDepth;
                } else if (exp.charAt(Ei) == '(') {
                    ++bracketDepth;
                }
            }
            if (bracketDepth != 0) {
                if (bracketDepth < 0) {
                    for (int Bi = 0; Bi < -bracketDepth; ++Bi) {
                        exp = "(" + exp;
                    }
                } else if (bracketDepth > 0) {
                    for (int Bi = 0; Bi < bracketDepth; ++Bi) {
                        exp = exp + ")";
                    }
                }
            }

            boolean parsing = true;
            boolean needsStitching = false;
            while (parsing && exp.charAt(0) == '(' && exp.charAt(exp.length() - 1) == ')') {
                bracketDepth = 0;
                needsStitching = true;
                for (int i = 0; i < exp.length(); ++i) {
                    if (exp.charAt(i) == ')') {
                        --bracketDepth;
                    } else if (exp.charAt(i) == '(') {
                        ++bracketDepth;
                    }
                    if (bracketDepth == 0 && i != exp.length() - 1) {
                        needsStitching = false;
                    }
                }
                if (needsStitching) {
                    exp = utility.removeHeadAndTail(exp);
                } else {
                    parsing = false;
                }
            }
            return exp;
        }


    }


}
