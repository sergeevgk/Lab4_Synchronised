public class Main {
    public static void main(String[] args){
        if (args[0] != null) {
            if (Log.init() != 0){
                return;
            }
            Log.report("Program started.");
            String fileName = args[0];
            Manager manager = new Manager(fileName);
            if (manager.openStreams() == 0) {
                if (manager.createWorkers() == 0) {
                    if (manager.introduceWorkers() == 0) {
                        manager.StartConveyor();
                    }
                }
                manager.closeStreams();
            }
            //log before exit to report about program'setConsumer work
            Log.report("Program finished.");
            Log.close();
        } else {
            Log.report("Missing command arguments.");
            Log.close();
            return;
        }
        return;
    }
}
