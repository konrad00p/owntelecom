package com.owntelecom.module;

import com.owntelecom.OwnTelecomPlugin;
import com.owntelecom.module.agreement.AgreementModule;
import com.owntelecom.module.call.CallModule;
import com.owntelecom.module.chat.ChatModule;
import com.owntelecom.module.internet.InternetModule;
import com.owntelecom.module.operator.OperatorModule;
import com.owntelecom.module.payment.PaymentModule;
import com.owntelecom.module.station.StationModule;
import com.owntelecom.service.CoverageService;
import com.owntelecom.service.StationIndex;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {

    private final OwnTelecomPlugin plugin;
    private final List<OwnTelecomModule> modules = new ArrayList<>();
    private StationIndex stationIndex;
    private CoverageService coverageService;

    private ChatModule chatModule;
    private OperatorModule operatorModule;
    private StationModule stationModule;
    private CallModule callModule;
    private InternetModule internetModule;
    private AgreementModule agreementModule;
    private PaymentModule paymentModule;

    public ModuleManager(OwnTelecomPlugin plugin) {
        this.plugin = plugin;
    }

    public void enableAll() {
        stationIndex = new StationIndex(plugin);
        stationIndex.rebuild();
        coverageService = new CoverageService(plugin, stationIndex);

        chatModule = register(new ChatModule());
        operatorModule = register(new OperatorModule());
        stationModule = register(new StationModule());
        callModule = register(new CallModule());
        internetModule = register(new InternetModule());
        agreementModule = register(new AgreementModule());
        paymentModule = register(new PaymentModule());

        for (OwnTelecomModule module : modules) {
            module.enable(plugin);
        }
    }

    public void disableAll() {
        for (int i = modules.size() - 1; i >= 0; i--) {
            modules.get(i).disable(plugin);
        }
    }

    private <T extends OwnTelecomModule> T register(T module) {
        modules.add(module);
        return module;
    }

    public StationIndex getStationIndex() {
        return stationIndex;
    }

    public CoverageService getCoverageService() {
        return coverageService;
    }

    public ChatModule getChatModule() {
        return chatModule;
    }

    public OperatorModule getOperatorModule() {
        return operatorModule;
    }

    public StationModule getStationModule() {
        return stationModule;
    }

    public CallModule getCallModule() {
        return callModule;
    }

    public InternetModule getInternetModule() {
        return internetModule;
    }

    public AgreementModule getAgreementModule() {
        return agreementModule;
    }

    public PaymentModule getPaymentModule() {
        return paymentModule;
    }
}
