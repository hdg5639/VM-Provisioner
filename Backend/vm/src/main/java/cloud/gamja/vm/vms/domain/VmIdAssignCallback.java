package cloud.gamja.vm.vms.domain;

import org.reactivestreams.Publisher;
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

import java.util.UUID;

@Component
public class VmIdAssignCallback implements BeforeConvertCallback<Vm> {

    @Override
    @NonNull
    public Publisher<Vm> onBeforeConvert(Vm vm, @NonNull SqlIdentifier table) {
        if (vm.getId() == null) {
            vm.setId(UUID.randomUUID());
        }
        return Mono.just(vm);
    }
}