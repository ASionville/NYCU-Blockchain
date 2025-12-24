package ethSC;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/LFDT-web3j/web3j/tree/main/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 1.7.0.
 */
@SuppressWarnings("rawtypes")
public class ChatRoom extends Contract {
    public static final String BINARY = "608060405234801561001057600080fd5b50604051610adf380380610adf83398101604081905261002f916100b2565b600080546001600160a01b03191633179055600161004d8282610207565b506000546040517f227306ab85e7ae6f2a821b8389aaf76c7aeaebbac0673f8c05c9ac6f4c58801f9161008e916001600160a01b03909116906001906102c5565b60405180910390a150610363565b634e487b7160e01b600052604160045260246000fd5b6000602082840312156100c457600080fd5b81516001600160401b038111156100da57600080fd5b8201601f810184136100eb57600080fd5b80516001600160401b038111156101045761010461009c565b604051601f8201601f19908116603f011681016001600160401b03811182821017156101325761013261009c565b60405281815282820160200186101561014a57600080fd5b60005b828110156101695760208185018101518383018201520161014d565b50600091810160200191909152949350505050565b600181811c9082168061019257607f821691505b6020821081036101b257634e487b7160e01b600052602260045260246000fd5b50919050565b601f82111561020257806000526020600020601f840160051c810160208510156101df5750805b601f840160051c820191505b818110156101ff57600081556001016101eb565b50505b505050565b81516001600160401b038111156102205761022061009c565b6102348161022e845461017e565b846101b8565b6020601f82116001811461026857600083156102505750848201515b600019600385901b1c1916600184901b1784556101ff565b600084815260208120601f198516915b828110156102985787850151825560209485019460019092019101610278565b50848210156102b65786840151600019600387901b60f8161c191681555b50505050600190811b01905550565b6001600160a01b038316815260406020820152815460009081906102e88161017e565b8060408601526001821660008114610307576001811461032357610357565b60ff1983166060870152606082151560051b8701019350610357565b86600052602060002060005b8381101561034e5781548882016060015260019091019060200161032f565b87016060019450505b50919695505050505050565b61076d806103726000396000f3fe608060405234801561001057600080fd5b506004361061007d5760003560e01c8063771b4c021161005b578063771b4c02146100d65780637d863fed146100eb578063873138f3146100fe578063d1d8665f1461011657600080fd5b8063042a4c1114610082578063469c8110146100a05780635e28ce58146100c3575b600080fd5b61008a61011e565b60405161009791906103f3565b60405180910390f35b6100b36100ae366004610423565b6101b0565b6040519015158152602001610097565b6100b36100d1366004610423565b610222565b6100e96100e4366004610423565b610285565b005b6100b36100f9366004610423565b6102f4565b3360009081526003602052604090205460ff166100b3565b61008a610360565b60606001805461012d906104dc565b80601f0160208091040260200160405190810160405280929190818152602001828054610159906104dc565b80156101a65780601f1061017b576101008083540402835291602001916101a6565b820191906000526020600020905b81548152906001019060200180831161018957829003601f168201915b5050505050905090565b3360009081526003602052604081205460ff161561021a57336000818152600260205260409081902090517fc607d1172496ae1addb7b7f21091f76fda0ff30f63a051237d8ab05b6e29811e9261020a9290918690610599565b60405180910390a1506001919050565b506000919050565b3360009081526003602052604081205460ff161561021a573360009081526002602052604090206102538382610628565b507f3bf1482492475f586e95d09728f501c1997357efa327d719fd2608116792863c338360405161020a9291906106e7565b6000546001600160a01b0316331461029c57600080fd5b60016102a88282610628565b506000546040517fb874b2b3d4aec1efc27e02496b5ea269e17a1b3d7bf8878d78a25766812f6213916102e9916001600160a01b0390911690600190610713565b60405180910390a150565b3360009081526003602052604081205460ff1661021a57336000908152600360205260409020805460ff1916600117905561032e82610222565b507fe5d4ce58d70016e45a65c0ac4176f20836975b47dac57ed370c1e730c58d49fc338360405161020a9291906106e7565b606061037b3360009081526003602052604090205460ff1690565b1561039a57336000908152600260205260409020805461012d906104dc565b5060408051602081019091526000815290565b6000815180845260005b818110156103d3576020818501810151868301820152016103b7565b506000602082860101526020601f19601f83011685010191505092915050565b60208152600061040660208301846103ad565b9392505050565b634e487b7160e01b600052604160045260246000fd5b60006020828403121561043557600080fd5b813567ffffffffffffffff81111561044c57600080fd5b8201601f8101841361045d57600080fd5b803567ffffffffffffffff8111156104775761047761040d565b604051601f8201601f19908116603f0116810167ffffffffffffffff811182821017156104a6576104a661040d565b6040528181528282016020018610156104be57600080fd5b81602084016020830137600091810160200191909152949350505050565b600181811c908216806104f057607f821691505b60208210810361051057634e487b7160e01b600052602260045260246000fd5b50919050565b60008154610523816104dc565b80855260018216801561053d576001811461055957610590565b60ff1983166020870152602082151560051b8701019350610590565b84600052602060002060005b838110156105875781546020828a010152600182019150602081019050610565565b87016020019450505b50505092915050565b6001600160a01b03841681526060602082018190526000906105bd90830185610516565b82810360408401526105cf81856103ad565b9695505050505050565b601f82111561062357806000526020600020601f840160051c810160208510156106005750805b601f840160051c820191505b81811015610620576000815560010161060c565b50505b505050565b815167ffffffffffffffff8111156106425761064261040d565b6106568161065084546104dc565b846105d9565b6020601f82116001811461068a57600083156106725750848201515b600019600385901b1c1916600184901b178455610620565b600084815260208120601f198516915b828110156106ba578785015182556020948501946001909201910161069a565b50848210156106d85786840151600019600387901b60f8161c191681555b50505050600190811b01905550565b6001600160a01b038316815260406020820181905260009061070b908301846103ad565b949350505050565b6001600160a01b038316815260406020820181905260009061070b9083018461051656fea26469706673582212206e7611990432898fb216b5a24758c703d682e12a0b4f9ac17ddf430c8b62176364736f6c634300081e0033";

    private static String librariesLinkedBinary;

    public static final String FUNC_ADDNEWUSER = "addNewUser";

    public static final String FUNC_CHANGECHATROOMNAME = "changeChatRoomName";

    public static final String FUNC_CHANGEUSERTITLE = "changeUserTitle";

    public static final String FUNC_GETCHATROOMNAME = "getChatRoomName";

    public static final String FUNC_GETUSERTITLE = "getUserTitle";

    public static final String FUNC_ISUSERREGISTERED = "isUserRegistered";

    public static final String FUNC_SENDMESSAGE = "sendMessage";

    public static final Event CHATROOMCREATED_EVENT = new Event("chatRoomCreated", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Utf8String>() {}));
    ;

    public static final Event CHATROOMNAMECHANGED_EVENT = new Event("chatRoomNameChanged", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Utf8String>() {}));
    ;

    public static final Event NEWMESSAGE_EVENT = new Event("newMessage", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}));
    ;

    public static final Event USERCHANGETITLE_EVENT = new Event("userChangeTitle", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Utf8String>() {}));
    ;

    public static final Event USERREGISTERED_EVENT = new Event("userRegistered", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Utf8String>() {}));
    ;

    @Deprecated
    protected ChatRoom(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected ChatRoom(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected ChatRoom(String contractAddress, Web3j web3j, TransactionManager transactionManager,
            BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected ChatRoom(String contractAddress, Web3j web3j, TransactionManager transactionManager,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<ChatRoomCreatedEventResponse> getChatRoomCreatedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(CHATROOMCREATED_EVENT, transactionReceipt);
        ArrayList<ChatRoomCreatedEventResponse> responses = new ArrayList<ChatRoomCreatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ChatRoomCreatedEventResponse typedResponse = new ChatRoomCreatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.roomOwner = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.roomName = (String) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static ChatRoomCreatedEventResponse getChatRoomCreatedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(CHATROOMCREATED_EVENT, log);
        ChatRoomCreatedEventResponse typedResponse = new ChatRoomCreatedEventResponse();
        typedResponse.log = log;
        typedResponse.roomOwner = (String) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.roomName = (String) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<ChatRoomCreatedEventResponse> chatRoomCreatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getChatRoomCreatedEventFromLog(log));
    }

    public Flowable<ChatRoomCreatedEventResponse> chatRoomCreatedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(CHATROOMCREATED_EVENT));
        return chatRoomCreatedEventFlowable(filter);
    }

    public static List<ChatRoomNameChangedEventResponse> getChatRoomNameChangedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(CHATROOMNAMECHANGED_EVENT, transactionReceipt);
        ArrayList<ChatRoomNameChangedEventResponse> responses = new ArrayList<ChatRoomNameChangedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ChatRoomNameChangedEventResponse typedResponse = new ChatRoomNameChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.roomOwner = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.newRoomName = (String) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static ChatRoomNameChangedEventResponse getChatRoomNameChangedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(CHATROOMNAMECHANGED_EVENT, log);
        ChatRoomNameChangedEventResponse typedResponse = new ChatRoomNameChangedEventResponse();
        typedResponse.log = log;
        typedResponse.roomOwner = (String) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.newRoomName = (String) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<ChatRoomNameChangedEventResponse> chatRoomNameChangedEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getChatRoomNameChangedEventFromLog(log));
    }

    public Flowable<ChatRoomNameChangedEventResponse> chatRoomNameChangedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(CHATROOMNAMECHANGED_EVENT));
        return chatRoomNameChangedEventFlowable(filter);
    }

    public static List<NewMessageEventResponse> getNewMessageEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(NEWMESSAGE_EVENT, transactionReceipt);
        ArrayList<NewMessageEventResponse> responses = new ArrayList<NewMessageEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            NewMessageEventResponse typedResponse = new NewMessageEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.userOwner = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.userTitle = (String) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.userMessage = (String) eventValues.getNonIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static NewMessageEventResponse getNewMessageEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(NEWMESSAGE_EVENT, log);
        NewMessageEventResponse typedResponse = new NewMessageEventResponse();
        typedResponse.log = log;
        typedResponse.userOwner = (String) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.userTitle = (String) eventValues.getNonIndexedValues().get(1).getValue();
        typedResponse.userMessage = (String) eventValues.getNonIndexedValues().get(2).getValue();
        return typedResponse;
    }

    public Flowable<NewMessageEventResponse> newMessageEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getNewMessageEventFromLog(log));
    }

    public Flowable<NewMessageEventResponse> newMessageEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(NEWMESSAGE_EVENT));
        return newMessageEventFlowable(filter);
    }

    public static List<UserChangeTitleEventResponse> getUserChangeTitleEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(USERCHANGETITLE_EVENT, transactionReceipt);
        ArrayList<UserChangeTitleEventResponse> responses = new ArrayList<UserChangeTitleEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            UserChangeTitleEventResponse typedResponse = new UserChangeTitleEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.userOwner = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.newUserTitle = (String) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static UserChangeTitleEventResponse getUserChangeTitleEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(USERCHANGETITLE_EVENT, log);
        UserChangeTitleEventResponse typedResponse = new UserChangeTitleEventResponse();
        typedResponse.log = log;
        typedResponse.userOwner = (String) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.newUserTitle = (String) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<UserChangeTitleEventResponse> userChangeTitleEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getUserChangeTitleEventFromLog(log));
    }

    public Flowable<UserChangeTitleEventResponse> userChangeTitleEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(USERCHANGETITLE_EVENT));
        return userChangeTitleEventFlowable(filter);
    }

    public static List<UserRegisteredEventResponse> getUserRegisteredEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(USERREGISTERED_EVENT, transactionReceipt);
        ArrayList<UserRegisteredEventResponse> responses = new ArrayList<UserRegisteredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            UserRegisteredEventResponse typedResponse = new UserRegisteredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.newUserOwner = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.newUserTitle = (String) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static UserRegisteredEventResponse getUserRegisteredEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(USERREGISTERED_EVENT, log);
        UserRegisteredEventResponse typedResponse = new UserRegisteredEventResponse();
        typedResponse.log = log;
        typedResponse.newUserOwner = (String) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.newUserTitle = (String) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<UserRegisteredEventResponse> userRegisteredEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getUserRegisteredEventFromLog(log));
    }

    public Flowable<UserRegisteredEventResponse> userRegisteredEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(USERREGISTERED_EVENT));
        return userRegisteredEventFlowable(filter);
    }

    public RemoteFunctionCall<TransactionReceipt> addNewUser(String _newUserTitle) {
        final Function function = new Function(
                FUNC_ADDNEWUSER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(_newUserTitle)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> changeChatRoomName(String _newChatRoomName) {
        final Function function = new Function(
                FUNC_CHANGECHATROOMNAME, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(_newChatRoomName)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> changeUserTitle(String _newUserTitle) {
        final Function function = new Function(
                FUNC_CHANGEUSERTITLE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(_newUserTitle)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> getChatRoomName() {
        final Function function = new Function(FUNC_GETCHATROOMNAME, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> getUserTitle() {
        final Function function = new Function(FUNC_GETUSERTITLE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<Boolean> isUserRegistered() {
        final Function function = new Function(FUNC_ISUSERREGISTERED, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<TransactionReceipt> sendMessage(String _message) {
        final Function function = new Function(
                FUNC_SENDMESSAGE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(_message)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static ChatRoom load(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        return new ChatRoom(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static ChatRoom load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new ChatRoom(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static ChatRoom load(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        return new ChatRoom(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static ChatRoom load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new ChatRoom(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<ChatRoom> deploy(Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider, String _chatRoomName) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(_chatRoomName)));
        return deployRemoteCall(ChatRoom.class, web3j, credentials, contractGasProvider, getDeploymentBinary(), encodedConstructor);
    }

    public static RemoteCall<ChatRoom> deploy(Web3j web3j, TransactionManager transactionManager,
            ContractGasProvider contractGasProvider, String _chatRoomName) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(_chatRoomName)));
        return deployRemoteCall(ChatRoom.class, web3j, transactionManager, contractGasProvider, getDeploymentBinary(), encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<ChatRoom> deploy(Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit, String _chatRoomName) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(_chatRoomName)));
        return deployRemoteCall(ChatRoom.class, web3j, credentials, gasPrice, gasLimit, getDeploymentBinary(), encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<ChatRoom> deploy(Web3j web3j, TransactionManager transactionManager,
            BigInteger gasPrice, BigInteger gasLimit, String _chatRoomName) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(_chatRoomName)));
        return deployRemoteCall(ChatRoom.class, web3j, transactionManager, gasPrice, gasLimit, getDeploymentBinary(), encodedConstructor);
    }

    public static void linkLibraries(List<Contract.LinkReference> references) {
        librariesLinkedBinary = linkBinaryWithReferences(BINARY, references);
    }

    private static String getDeploymentBinary() {
        if (librariesLinkedBinary != null) {
            return librariesLinkedBinary;
        } else {
            return BINARY;
        }
    }

    public static class ChatRoomCreatedEventResponse extends BaseEventResponse {
        public String roomOwner;

        public String roomName;
    }

    public static class ChatRoomNameChangedEventResponse extends BaseEventResponse {
        public String roomOwner;

        public String newRoomName;
    }

    public static class NewMessageEventResponse extends BaseEventResponse {
        public String userOwner;

        public String userTitle;

        public String userMessage;
    }

    public static class UserChangeTitleEventResponse extends BaseEventResponse {
        public String userOwner;

        public String newUserTitle;
    }

    public static class UserRegisteredEventResponse extends BaseEventResponse {
        public String newUserOwner;

        public String newUserTitle;
    }
}
