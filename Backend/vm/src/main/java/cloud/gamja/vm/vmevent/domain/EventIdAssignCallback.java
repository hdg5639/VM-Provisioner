package cloud.gamja.vm.vmevent.domain;

import org.reactivestreams.Publisher;
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

import java.util.UUID;

@Component
public class EventIdAssignCallback implements BeforeConvertCallback<VmEvent> {

    @Override
    @NonNull
    public Publisher<VmEvent> onBeforeConvert(VmEvent vmEvent, @NonNull SqlIdentifier table) {
        if (vmEvent.getId() == null) {
            vmEvent.setId(UUID.randomUUID());
        }
        return Mono.just(vmEvent);
    }
}