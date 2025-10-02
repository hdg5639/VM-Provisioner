package cloud.gamja.vm.vmkey.domain;

import org.reactivestreams.Publisher;
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

import java.util.UUID;

@Component
public class VmKeyIdAssignCallback implements BeforeConvertCallback<VmKey> {

    @Override
    @NonNull
    public Publisher<VmKey> onBeforeConvert(VmKey vmKey, @NonNull SqlIdentifier table) {
        if (vmKey.getId() == null) {
            vmKey.setId(UUID.randomUUID());
        }
        return Mono.just(vmKey);
    }
}