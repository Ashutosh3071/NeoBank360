import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { ContactComponent } from './contact';

describe('ContactComponent', () => {
  let component: ContactComponent;
  let fixture: ComponentFixture<ContactComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ContactComponent],
      providers: [
        provideRouter([])
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ContactComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have invalid form initially', () => {
    expect(component.contactForm.valid).toBeFalsy();
  });

  it('should validate form fields', () => {
    const name = component.contactForm.controls['name'];
    const email = component.contactForm.controls['email'];
    const subject = component.contactForm.controls['subject'];
    const message = component.contactForm.controls['message'];

    expect(name.valid).toBeFalsy();

    name.setValue('John Doe');
    email.setValue('invalid-email');
    subject.setValue('Help');
    message.setValue('Short');

    expect(email.valid).toBeFalsy();
    expect(subject.valid).toBeFalsy(); // subject min length 5
    expect(message.valid).toBeFalsy(); // message min length 10

    email.setValue('john@example.com');
    subject.setValue('Inquiry about account');
    message.setValue('This is a longer contact message that meets the minimum length requirement.');

    expect(component.contactForm.valid).toBeTruthy();
  });
});
