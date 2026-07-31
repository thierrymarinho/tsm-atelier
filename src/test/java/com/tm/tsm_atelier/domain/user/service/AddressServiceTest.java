package com.tm.tsm_atelier.domain.user.service;

import static com.tm.tsm_atelier.common.builders.UserBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.tm.tsm_atelier.common.exception.custom.AddressLimitExceededException;
import com.tm.tsm_atelier.common.exception.custom.AddressNotFoundException;
import com.tm.tsm_atelier.domain.user.dto.AddressRequestDTO;
import com.tm.tsm_atelier.domain.user.dto.AddressResponseDTO;
import com.tm.tsm_atelier.domain.user.entity.Address;
import com.tm.tsm_atelier.domain.user.entity.User;
import com.tm.tsm_atelier.domain.user.enums.State;
import com.tm.tsm_atelier.domain.user.repository.AddressRepository;
import com.tm.tsm_atelier.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

	@InjectMocks
	private AddressService addressService;

	@Mock
	private AddressRepository addressRepository;

	@Mock
	private UserRepository userRepository;

	private User createUser() {
		return aUser().withId(UUID.randomUUID()).build();
	}

	private AddressRequestDTO createRequest(boolean isDefault) {
		return new AddressRequestDTO("Street", "123", "Comp", "Neighborhood", "City", State.SP, "12345-678", isDefault);
	}

	@Nested
	@DisplayName("create()")
	class Create {

		@Test
		@DisplayName("Deve criar o primeiro endereço como default, independentemente do request")
		void shouldCreateFirstAddressAsDefault() {
			User user = createUser();
			AddressRequestDTO request = createRequest(false);

			when(userRepository.findByIdWithPessimisticLock(user.getId())).thenReturn(Optional.of(user));
			when(addressRepository.countByUserId(user.getId())).thenReturn(0L);
			when(addressRepository.save(any(Address.class))).thenAnswer(i -> {
				Address a = i.getArgument(0);
				a.setId(1L);
				return a;
			});

			AddressResponseDTO result = addressService.create(user, request);

			assertThat(result.isDefault()).isTrue();
			assertThat(result.postalCode()).isEqualTo("12345678"); // formatZipCode must apply

			ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
			verify(addressRepository).save(captor.capture());
			assertThat(captor.getValue().isDefault()).isTrue();
			assertThat(captor.getValue().getPostalCode()).isEqualTo("12345678");
		}

		@Test
		@DisplayName("Deve lançar erro quando usuário já tem 5 endereços")
		void shouldThrowWhenLimitExceeded() {
			User user = createUser();
			AddressRequestDTO request = createRequest(false);

			when(userRepository.findByIdWithPessimisticLock(user.getId())).thenReturn(Optional.of(user));
			when(addressRepository.countByUserId(user.getId())).thenReturn(5L);

			assertThatThrownBy(() -> addressService.create(user, request))
					.isInstanceOf(AddressLimitExceededException.class)
					.hasMessageContaining("You can only have up to 5 addresses");

			verify(addressRepository, never()).save(any());
		}

		@Test
		@DisplayName("Deve limpar o default antigo quando novo endereço é criado como default")
		void shouldClearOldDefaultWhenNewIsDefault() {
			User user = createUser();
			AddressRequestDTO request = createRequest(true);

			Address oldDefault = Address.builder().id(1L).isDefault(true).build();

			when(userRepository.findByIdWithPessimisticLock(user.getId())).thenReturn(Optional.of(user));
			when(addressRepository.countByUserId(user.getId())).thenReturn(1L);
			when(addressRepository.findByUserIdAndIsDefaultTrue(user.getId())).thenReturn(Optional.of(oldDefault));
			when(addressRepository.save(any(Address.class))).thenAnswer(i -> i.getArgument(0));

			addressService.create(user, request);

			verify(addressRepository).saveAndFlush(oldDefault);
			assertThat(oldDefault.isDefault()).isFalse();

			ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
			verify(addressRepository, times(1)).save(captor.capture());
			assertThat(captor.getValue().isDefault()).isTrue();
		}
	}

	@Nested
	@DisplayName("delete()")
	class Delete {

		@Test
		@DisplayName("Deve apagar endereço não-default sem afetar os outros")
		void shouldDeleteNonDefaultAddress() {
			User user = createUser();
			Long addrId = 1L;
			Address addr = Address.builder().id(addrId).user(user).isDefault(false).build();

			when(addressRepository.findById(addrId)).thenReturn(Optional.of(addr));

			addressService.delete(user, addrId);

			verify(addressRepository).delete(addr);
			verify(addressRepository, never()).findByUserIdOrderByCreatedAtAsc(any());
		}

		@Test
		@DisplayName("Deve promover outro endereço a default quando o default é apagado")
		void shouldPromoteNewDefaultWhenDefaultIsDeleted() {
			User user = createUser();
			Long addrId = 1L;
			Address addr = Address.builder().id(1L).user(user).street("Street").number("123").city("City")
					.state(State.SP).postalCode("12345-678").isDefault(true).build();
			Address newDefault = Address.builder().id(2L).user(user).isDefault(false).build();

			when(addressRepository.findById(addrId)).thenReturn(Optional.of(addr));
			when(addressRepository.findByUserIdOrderByCreatedAtAsc(user.getId())).thenReturn(List.of(newDefault));

			addressService.delete(user, addrId);

			verify(addressRepository).delete(addr);
			verify(addressRepository).save(newDefault);
			assertThat(newDefault.isDefault()).isTrue();
		}

		@Test
		@DisplayName("Deve lançar erro se endereço não pertence ao usuário")
		void shouldThrowIfAddressNotBelongsToUser() {
			User user = createUser();
			User anotherUser = aUser().withId(UUID.randomUUID()).build();
			Long addrId = 1L;
			Address addr = Address.builder().id(addrId).user(anotherUser).build();

			when(addressRepository.findById(addrId)).thenReturn(Optional.of(addr));

			assertThatThrownBy(() -> addressService.delete(user, addrId)).isInstanceOf(AddressNotFoundException.class);
		}
	}
}
