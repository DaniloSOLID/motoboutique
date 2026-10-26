package com.motoboutique.seed;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class DataSeederTest {

    @Test
    void ejecutaLasFasesEnElOrdenRecibido() {
        SeederFase primera = mock(SeederFase.class);
        SeederFase segunda = mock(SeederFase.class);

        new DataSeeder(List.of(primera, segunda), true).run(null);

        InOrder orden = inOrder(primera, segunda);
        orden.verify(primera).ejecutar();
        orden.verify(segunda).ejecutar();
    }

    @Test
    void desactivadoNoEjecutaNingunaFase() {
        SeederFase fase = mock(SeederFase.class);

        new DataSeeder(List.of(fase), false).run(null);

        verifyNoInteractions(fase);
    }
}
