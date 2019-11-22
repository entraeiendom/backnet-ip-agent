package no.entra.bacnet.agent.rec;

import com.serotonin.bacnet4j.exception.IllegalPduTypeException;
import no.entra.bacnet.agent.RealEstateCoreMessage;
import no.entra.bacnet.agent.recording.BacnetHexStringRecorder;
import no.entra.bacnet.agent.recording.FileBacnetHexStringRecorder;
import no.entra.bacnet.json.BacNetParser;
import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.slf4j.LoggerFactory.getLogger;

public class ProcessRecordedFile implements Bacnet2RealEstateCore {
    private static final Logger log = getLogger(ProcessRecordedFile.class);

    private final File recodedFile;
    private final BacNetParser bacNetParser;
    private final BacnetHexStringRecorder recorder;

    public ProcessRecordedFile(File recodedFile) {
        this(recodedFile, new BacNetParser());
    }

    public ProcessRecordedFile(File recodedFile, BacNetParser bacNetParser) {
        this.recodedFile = recodedFile;
        recorder = new FileBacnetHexStringRecorder(recodedFile);
        this.bacNetParser = bacNetParser;
    }

    @Override
    public RealEstateCoreMessage buildFromBacnetJson(String bacnetJson) {
        return null;
    }

    public void writeToFile(String hexString) {
        //FIXME write to file
    }

    public List<RealEstateCoreMessage> fetchFromFile() {

        List<RealEstateCoreMessage> messages = new ArrayList<>();
        Stream<String> bacnetHexStream = recorder.read();
        List<String> bacnetHexStrings = bacnetHexStream.collect(Collectors.toList());
        ArrayList<String> arrayList = new ArrayList<>(bacnetHexStrings);
        for (String hexString : arrayList) {
            RealEstateCoreMessage message = buildHexString(hexString);
            if (message != null) {
                messages.add(message);
            }
        }
        return messages;
    }

    private RealEstateCoreMessage buildHexString(String hexString) {
        RealEstateCoreMessage message = new RealEstateCoreMessage(hexString);
        try {
            String apduHexString = findApduHexString(hexString);
            String json = bacNetParser.jasonFromApdu(apduHexString);
            message.setBacknetJson(json);
            log.debug("Did build message from {}", hexString);
        } catch (IllegalArgumentException e) {
            log.debug("Failed to build json from {}. Reason: {}", hexString, e.getMessage());
        } catch (IllegalPduTypeException e) {
            log.debug("Could not build APDU from {}. Reason: {}", hexString, e.getMessage());
        } catch (Exception e) {
            log.debug("Failed to create message from {}. Reason: {}", hexString, e.getMessage());
        }

        return message;
    }

    String findApduHexString(String hexString) {
        String apduHexString = null;
        if (hexString != null && hexString.startsWith("81")) {
            apduHexString = hexString.substring(10);
        } else {
            apduHexString = hexString;
        }
        return apduHexString;
    }


}