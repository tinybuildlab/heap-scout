package io.heapscout.app.application.service

import io.heapscout.app.application.port.LocalHeapDumpPickerPort
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DisplayName("LocalHeapDumpSelectionService: 시스템 파일 선택 조정")
class LocalHeapDumpSelectionServiceTest {
    @Nested
    @DisplayName("정책: 파일 선택 결과를 실행 방식과 무관한 경로로 반환해야 한다")
    inner class SelectionPolicy {
        @Test
        @DisplayName("사용자가 파일을 선택하면 해당 경로를 반환한다")
        fun select_selectedFile_returnsPath() {
            val selected = Path.of("/tmp/example.hprof")
            val sut = LocalHeapDumpSelectionService(LocalHeapDumpPickerPort { selected })

            val result = sut.select()

            assertEquals(selected, result)
        }

        @Test
        @DisplayName("사용자가 대화상자를 취소하면 null을 반환한다")
        fun select_cancelledDialog_returnsNull() {
            val sut = LocalHeapDumpSelectionService(LocalHeapDumpPickerPort { null })

            val result = sut.select()

            assertNull(result)
        }

        @Test
        @DisplayName("데스크톱 창을 열 수 없는 환경이면 파일 선택기 사용 가능 여부는 false이다")
        fun isAvailable_headlessPicker_returnsFalse() {
            val unavailablePicker = object : LocalHeapDumpPickerPort {
                override fun pick(): Path? = null

                override fun isAvailable(): Boolean = false
            }
            val sut = LocalHeapDumpSelectionService(unavailablePicker)

            val available = sut.isAvailable()

            assertEquals(false, available)
        }
    }

    @Nested
    @DisplayName("정책: 시스템 파일 선택 대화상자는 한 번에 하나만 열어야 한다")
    inner class ConcurrencyPolicy {
        @Test
        @DisplayName("첫 대화상자가 열려 있을 때 두 번째 요청은 즉시 거절한다")
        fun select_pickerAlreadyOpen_throwsBusyException() {
            val pickerStarted = CountDownLatch(1)
            val releasePicker = CountDownLatch(1)
            val sut = LocalHeapDumpSelectionService(
                LocalHeapDumpPickerPort {
                    pickerStarted.countDown()
                    releasePicker.await(2, TimeUnit.SECONDS)
                    Path.of("/tmp/example.hprof")
                },
            )
            val executor = Executors.newSingleThreadExecutor()

            try {
                val firstSelection = executor.submit<Path?> { sut.select() }
                assertTrue(pickerStarted.await(1, TimeUnit.SECONDS))

                assertFailsWith<LocalFilePickerBusyException> {
                    sut.select()
                }

                releasePicker.countDown()
                assertEquals(Path.of("/tmp/example.hprof"), firstSelection.get(1, TimeUnit.SECONDS))
            } finally {
                releasePicker.countDown()
                executor.shutdownNow()
            }
        }
    }
}
